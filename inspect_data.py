import pandas as pd
import os

datasets = [
    'datasets/Spam_SMS.csv',
    'datasets/phishing_dataset_with_category.csv',
    'datasets/india_fraud_detection_FINAL.csv'
]

for d in datasets:
    print(f"\n--- {d} ---")
    if os.path.exists(d):
        df = pd.read_csv(d)
        print("Columns:", df.columns.tolist())
        print("Shape:", df.shape)
        print("Head:")
        print(df.head(3))
        # Find label column if obvious to show unique values
        for col in df.columns:
            if 'label' in col.lower() or 'class' in col.lower() or 'category' in col.lower() or 'type' in col.lower() or 'target' in col.lower():
                print(f"Unique values in {col}:", df[col].unique())
    else:
        print("FILE NOT FOUND")
