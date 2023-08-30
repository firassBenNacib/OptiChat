import pandas as pd
from datetime import datetime


df = pd.read_csv('merged_rep.arff', skiprows=10, header=None, comment='@')


columns = [
    "Timestamp", "Message", "'MessageNumber'", "'Queue Size'", "'Queue Difference Metric'", 
    "'Latency (ms)'", "'Message Size (bytes)'", "'Throughput (messages/sec)'", "'Used Memory (bytes)'"
]
df.columns = columns


df['Timestamp'] = pd.to_datetime(df['Timestamp'])


df['Elapsed_Seconds'] = (df['Timestamp'] - df['Timestamp'].iloc[0]).dt.total_seconds()


df = df.drop('Timestamp', axis=1)


df = df[['Elapsed_Seconds'] + [col for col in df if col != 'Elapsed_Seconds']]


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
