import os
import torch
from transformers import AutoTokenizer, AutoModelForSequenceClassification

def export_to_onnx(model_path: str, output_path: str):
    print(f"Loading model from {model_path} for ONNX export...")
    tokenizer = AutoTokenizer.from_pretrained(model_path)
    model = AutoModelForSequenceClassification.from_pretrained(model_path)
    model.eval()

    dummy_text = "This is a dummy input for ONNX tracing."
    inputs = tokenizer(dummy_text, return_tensors="pt", max_length=128, padding="max_length", truncation=True)

    input_names = ["input_ids", "attention_mask"]
    output_names = ["logits"]
    
    dynamic_axes = {
        "input_ids": {0: "batch_size", 1: "sequence_length"},
        "attention_mask": {0: "batch_size", 1: "sequence_length"},
        "logits": {0: "batch_size"}
    }

    os.makedirs(output_path, exist_ok=True)
    onnx_file = os.path.join(output_path, "model.onnx")
    
    print(f"Exporting ONNX model to {onnx_file}...")
    torch.onnx.export(
        model,
        (inputs["input_ids"], inputs["attention_mask"]),
        onnx_file,
        export_params=True,
        opset_version=14,
        do_constant_folding=True,
        input_names=input_names,
        output_names=output_names,
        dynamic_axes=dynamic_axes
    )
    
    print("Quantizing ONNX model to INT8...")
    import onnxruntime
    from onnxruntime.quantization import quantize_dynamic, QuantType
    
    quantized_model_path = os.path.join(output_path, "model_quantized.onnx")
    quantize_dynamic(
        model_input=onnx_file,
        model_output=quantized_model_path,
        weight_type=QuantType.QInt8
    )
    print(f"Quantized model saved to {quantized_model_path}")
    
    # Save tokenizer for ONNX wrapper to use
    tokenizer.save_pretrained(output_path)
    print("ONNX export complete.")

if __name__ == "__main__":
    export_to_onnx("../../cipher_distilbert_detection", "./model_onnx")
