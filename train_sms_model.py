import os
import json
import logging
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, precision_recall_curve
from sklearn.utils.class_weight import compute_class_weight
import matplotlib.pyplot as plt
import torch
from datasets import Dataset
from transformers import (
    AutoTokenizer, 
    AutoModelForSequenceClassification, 
    TrainingArguments, 
    Trainer
)

# Setup Logging
logging.basicConfig(level=logging.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

def load_and_normalize_data():
    logging.info("Loading datasets...")
    
    # 1. Spam_SMS.csv
    # Columns: Class, Message. Class: 'spam' -> 1, 'ham' -> 0
    df1 = pd.read_csv('datasets/Spam_SMS.csv')
    df1 = df1.rename(columns={'Message': 'text'})
    df1['label'] = df1['Class'].apply(lambda x: 1 if str(x).lower().strip() == 'spam' else 0)
    df1 = df1[['text', 'label']]
    
    # 2. phishing_dataset_with_category.csv
    # Columns: text, category, label ('phishing' -> 1)
    df2 = pd.read_csv('datasets/phishing_dataset_with_category.csv')
    df2['label'] = 1 # all in this dataset are phishing
    df2 = df2[['text', 'label']]
    
    # 3. india_fraud_detection_FINAL.csv
    # Columns: message_text, category, subcategory, language
    # Categories ending with '_Legitimate' -> 0, else -> 1
    df3 = pd.read_csv('datasets/india_fraud_detection_FINAL.csv')
    df3 = df3.rename(columns={'message_text': 'text'})
    df3['label'] = df3['category'].apply(lambda x: 0 if 'legitimate' in str(x).lower() else 1)
    df3 = df3[['text', 'label']]
    
    # Combine
    combined_df = pd.concat([df1, df2, df3], ignore_index=True)
    
    # Drop NaNs
    initial_len = len(combined_df)
    combined_df = combined_df.dropna(subset=['text', 'label'])
    
    # Convert types
    combined_df['text'] = combined_df['text'].astype(str)
    combined_df['label'] = combined_df['label'].astype(int)
    
    # Drop duplicates
    combined_df = combined_df.drop_duplicates(subset=['text'])
    
    logging.info(f"Dropped {initial_len - len(combined_df)} duplicates/nulls.")
    logging.info(f"Final dataset size: {len(combined_df)}")
    
    # Class Distribution
    dist = combined_df['label'].value_counts(normalize=True).to_dict()
    logging.info(f"Class distribution: {dist}")
    
    imbalance = max(dist.values())
    class_weights_dict = None
    if imbalance > 0.70:
        logging.info("Imbalance > 70/30 detected. Computing class weights.")
        weights = compute_class_weight(
            class_weight='balanced', 
            classes=np.unique(combined_df['label']), 
            y=combined_df['label'].values
        )
        class_weights_dict = {i: w for i, w in enumerate(weights)}
        logging.info(f"Computed weights: {class_weights_dict}")
    else:
        logging.info("Data is sufficiently balanced. No class weights needed.")
        
    return combined_df, class_weights_dict

def compute_metrics(eval_pred):
    logits, labels = eval_pred
    predictions = np.argmax(logits, axis=-1)
    precision = precision_score(labels, predictions, zero_division=0)
    recall = recall_score(labels, predictions, zero_division=0)
    f1 = f1_score(labels, predictions, zero_division=0)
    acc = accuracy_score(labels, predictions)
    return {
        'accuracy': acc,
        'f1': f1,
        'precision': precision,
        'recall': recall
    }

class CustomTrainer(Trainer):
    def __init__(self, class_weights=None, *args, **kwargs):
        super().__init__(*args, **kwargs)
        if class_weights is not None:
            weights_tensor = torch.tensor([class_weights[0], class_weights[1]], dtype=torch.float)
            self.class_weights = weights_tensor.to(self.args.device)
        else:
            self.class_weights = None

    def compute_loss(self, model, inputs, return_outputs=False, **kwargs):
        labels = inputs.pop("labels")
        outputs = model(**inputs)
        logits = outputs.logits
        if self.class_weights is not None:
            loss_fct = torch.nn.CrossEntropyLoss(weight=self.class_weights)
            loss = loss_fct(logits.view(-1, self.model.config.num_labels), labels.view(-1))
        else:
            loss_fct = torch.nn.CrossEntropyLoss()
            loss = loss_fct(logits.view(-1, self.model.config.num_labels), labels.view(-1))
        return (loss, outputs) if return_outputs else loss

def calibrate_threshold(trainer, val_dataset):
    logging.info("Calibrating decision threshold for >= 95% Precision...")
    predictions = trainer.predict(val_dataset)
    logits = predictions.predictions
    # Convert logits to probabilities using softmax
    probs = torch.nn.functional.softmax(torch.tensor(logits), dim=-1).numpy()
    pos_probs = probs[:, 1]
    labels = predictions.label_ids
    
    precisions, recalls, thresholds = precision_recall_curve(labels, pos_probs)
    
    # Find the lowest threshold where precision >= 0.95
    optimal_idx = np.argmax(precisions >= 0.95)
    
    if precisions[optimal_idx] >= 0.95 and optimal_idx < len(thresholds):
        optimal_threshold = float(thresholds[optimal_idx])
    else:
        # Fallback to default if 95% not achievable (unlikely with deep learning but possible)
        optimal_threshold = 0.5
        logging.warning("Could not achieve 95% precision on validation set. Using default 0.5 threshold.")
        
    final_precision = float(precisions[optimal_idx])
    final_recall = float(recalls[optimal_idx])
    
    logging.info(f"Optimal Threshold: {optimal_threshold:.4f} | Precision: {final_precision:.4f} | Recall: {final_recall:.4f}")
    
    # Standard confusion matrix at optimal threshold
    final_preds = (pos_probs >= optimal_threshold).astype(int)
    cm = confusion_matrix(labels, final_preds)
    
    return optimal_threshold, float(final_precision), float(final_recall), cm

def evaluate_robustness(model, tokenizer, optimal_threshold, device):
    logging.info("Running robustness checks...")
    adversarial_tests = [
        {"type": "URL Obfuscation", "text": "Win $1000 fast! Click hxxp://scammy-link[.]com", "expected": 1},
        {"type": "URL Obfuscation", "text": "Your account is locked. Verify at bit.ly/secure-login-392", "expected": 1},
        {"type": "Numeric Replacement", "text": "W1NNER! Y0u have b33n select3d for a pr1ze.", "expected": 1},
        {"type": "Mixed Case", "text": "cOnGrAtUlAtIoNs, yOu WoN a FrEe iPhOnE!", "expected": 1},
        {"type": "Short SMS", "text": "Call me now", "expected": 1}, # Could be spam hook
        {"type": "Short SMS", "text": "Hey", "expected": 0} # Normal short SMS
    ]
    
    model.eval()
    results = []
    
    for test in adversarial_tests:
        inputs = tokenizer(test["text"], return_tensors="pt", truncation=True, max_length=128, padding=True)
        inputs = {k: v.to(device) for k, v in inputs.items()}
        with torch.no_grad():
            outputs = model(**inputs)
            probs = torch.nn.functional.softmax(outputs.logits, dim=-1)[0].cpu().numpy()
            pos_prob = probs[1]
            pred = 1 if pos_prob >= optimal_threshold else 0
            
            passed = (pred == test["expected"])
            results.append({
                "type": test["type"],
                "text": test["text"],
                "probability": float(pos_prob),
                "predicted": int(pred),
                "expected": test["expected"],
                "passed": passed
            })
            
    failure_count = sum(1 for r in results if not r['passed'])
    logging.info(f"Robustness failures: {failure_count}/{len(adversarial_tests)}")
    for r in results:
        if not r['passed']:
            logging.warning(f"Failed case: {r['type']} - Prob: {r['probability']:.3f} - Text: '{r['text']}'")
            
    return results

def main():
    df, class_weights = load_and_normalize_data()
    
    # Train / Val Split (90/10 stratified)
    train_df, val_df = train_test_split(df, test_size=0.1, stratify=df['label'], random_state=42)
    
    # HF Datasets
    train_dataset = Dataset.from_pandas(train_df, preserve_index=False)
    val_dataset = Dataset.from_pandas(val_df, preserve_index=False)
    
    # Tokenizer
    model_id = "distilbert-base-uncased"
    tokenizer = AutoTokenizer.from_pretrained(model_id)
    
    def tokenize_fn(examples):
        return tokenizer(examples['text'], padding="max_length", truncation=True, max_length=128)
        
    train_dataset = train_dataset.map(tokenize_fn, batched=True)
    val_dataset = val_dataset.map(tokenize_fn, batched=True)
    
    # Model Setup
    model = AutoModelForSequenceClassification.from_pretrained(model_id, num_labels=2)
    device = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
    model.to(device)
    
    # Training Arguments
    Gradient_Accumulation = 1
    if len(df) > 100000:
        Gradient_Accumulation = 2
        logging.info("Dataset > 100k, enabled gradient accumulation")
        
    training_args = TrainingArguments(
        output_dir='./results',
        learning_rate=2e-5,
        per_device_train_batch_size=16,
        per_device_eval_batch_size=32,
        num_train_epochs=3,
        weight_decay=0.01,
        eval_strategy="epoch",
        save_strategy="epoch",
        load_best_model_at_end=True,
        metric_for_best_model="f1",
        gradient_accumulation_steps=Gradient_Accumulation,
        max_grad_norm=1.0,
        fp16=torch.cuda.is_available(), # Mixed precision
        logging_dir='./logs',
        logging_steps=50,
        seed=42
    )
    
    trainer = CustomTrainer(
        model=model,
        args=training_args,
        train_dataset=train_dataset,
        eval_dataset=val_dataset,
        compute_metrics=compute_metrics,
        class_weights=class_weights
    )
    
    # Train
    logging.info("Starting training...")
    trainer.train()
    
    # Standard Eval
    logging.info("Evaluating optimal F1 model...")
    eval_metrics = trainer.evaluate()
    
    # Threshold Calibration
    opt_thresh, p_cal, r_cal, cm = calibrate_threshold(trainer, val_dataset)
    
    # Save Confusion Matrix
    out_dir = './cipher_distilbert_detection'
    os.makedirs(out_dir, exist_ok=True)
    
    plt.figure(figsize=(6, 5))
    plt.imshow(cm, interpolation='nearest', cmap=plt.cm.Blues)
    plt.title('Confusion matrix at optimal threshold')
    plt.colorbar()
    tick_marks = np.arange(2)
    plt.xticks(tick_marks, ['Legitimate (0)', 'Scam (1)'])
    plt.yticks(tick_marks, ['Legitimate (0)', 'Scam (1)'])
    
    thresh = cm.max() / 2.
    for i in range(cm.shape[0]):
        for j in range(cm.shape[1]):
            plt.text(j, i, format(cm[i, j], 'd'),
                     horizontalalignment="center",
                     color="white" if cm[i, j] > thresh else "black")
    plt.tight_layout()
    plt.ylabel('True label')
    plt.xlabel('Predicted label')
    plt.savefig(os.path.join(out_dir, 'confusion_matrix.png'))
    plt.close()
    
    # Robustness checks
    robustness_results = evaluate_robustness(model, tokenizer, opt_thresh, device)
    
    # Model Export
    logging.info("Saving model pipeline...")
    model.save_pretrained(out_dir)
    tokenizer.save_pretrained(out_dir)
    
    # Deployment recommendation
    model_size_mb = sum(p.numel() for p in model.parameters() if p.requires_grad) * 4 / (1024 ** 2)
    
    # Given DistilBERT is fast, CPU is often acceptable for low throughput, but GPU is better for batching
    cpu_latency = "~10-30ms / sequence"
    gpu_latency = "~2-5ms / sequence"
    deployment_rec = "CPU" if model_size_mb < 300 else "GPU" # simplistic proxy
    if torch.cuda.is_available(): # if you have a GPU right now, you can likely deploy there
        deployment_rec = "GPU (FastAPI wrapped, dynamic batching recommended)"
    else:
        deployment_rec = "CPU (FastAPI wrapped, ONNX quantization recommended)"

    final_report = {
        "dataset_stats": {
            "total_samples": len(df),
            "train_size": len(train_df),
            "val_size": len(val_df),
            "class_distribution_raw": df['label'].value_counts(normalize=True).to_dict()
        },
        "training_metrics": {
            "best_f1": eval_metrics.get("eval_f1", 0.0),
            "best_model_accuracy": eval_metrics.get("eval_accuracy", 0.0)
        },
        "calibration": {
            "optimal_threshold": opt_thresh,
            "calibrated_precision": p_cal,
            "calibrated_recall": r_cal,
            "class_weights_used": class_weights
        },
        "robustness": robustness_results,
        "deployment": {
            "model_size_mb": round(model_size_mb, 2),
            "cpu_latency_estimate": cpu_latency,
            "gpu_latency_estimate": gpu_latency,
            "recommendation": deployment_rec
        },
        "model_path": out_dir
    }
    
    with open(os.path.join(out_dir, 'training_metrics.json'), 'w') as f:
        json.dump(final_report, f, indent=4)
        
    logging.info("=== FINAL REPORT ===")
    print(json.dumps(final_report, indent=4))
    logging.info("Pipeline Complete.")

if __name__ == "__main__":
    main()
