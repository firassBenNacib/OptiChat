import pandas as pd
from datetime import datetime

# Load ARFF file into a DataFrame, skip the metadata at the top
df = pd.read_csv('merged_rep.arff', skiprows=10, header=None, comment='@')

# Rename columns for easier manipulation
columns = [
    "Timestamp", "Message", "'MessageNumber'", "'Queue Size'", "'Queue Difference Metric'", 
    "'Latency (ms)'", "'Message Size (bytes)'", "'Throughput (messages/sec)'", "'Used Memory (bytes)'"
]
df.columns = columns

# Convert the Timestamp column to datetime format
df['Timestamp'] = pd.to_datetime(df['Timestamp'])

# Create a new column with elapsed seconds from the start
df['Elapsed_Seconds'] = (df['Timestamp'] - df['Timestamp'].iloc[0]).dt.total_seconds()

# Drop the original Timestamp column
df = df.drop('Timestamp', axis=1)

# Move the Elapsed_Seconds column to the start
df = df[['Elapsed_Seconds'] + [col for col in df if col != 'Elapsed_Seconds']]

# Save the DataFrame back to ARFF format
with open('processed_merged_rep.arff', 'w') as f:
    f.write('@relation merged_rep\n\n')
    for column in df.columns:
        if df[column].dtype == 'float64':
            f.write(f'@attribute {column} numeric\n')
        elif df[column].dtype == 'int64':
            f.write(f'@attribute {column} numeric\n')
        else:
            values = ','.join(df[column].astype(str).unique())
            f.write(f'@attribute {column} {{{values}}}\n')
    f.write('\n@data\n')
    df.to_csv(f, index=False, header=False)
