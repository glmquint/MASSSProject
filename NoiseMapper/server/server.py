from flask import Flask,request, send_from_directory
import sqlite3
from time import time
app = Flask(__name__)
conn = sqlite3.connect('db/mapdata.db', check_same_thread=False)
c = conn.cursor()

# Create a table if it doesn't exist
c.execute('''CREATE TABLE IF NOT EXISTS noise_measurements
             (timestamp INTEGER NOT NULL, room TEXT, noise REAL)''')
conn.commit()

@app.route('/measurements', methods=['POST'])
def save_request():
    data = request.json
    if data:
        # Save the request data to the database
        c.execute("INSERT INTO noise_measurements (timestamp, room, noise) VALUES (?,?,?)", (int(time()), data['room'], data['noise']))
        conn.commit()
        return 'Request saved successfully'
    else:
        return 'No data provided'

@app.route('/measurements', methods=['GET'])
def get_requests():
    # Retrieve all requests from the database
    c.execute("SELECT * FROM noise_measurements ORDER BY timestamp DESC")
    rows = c.fetchall()
    return rows

@app.route('/resources/<filename>')
def download_file(filename):
    try:
        return send_from_directory('resources', filename)
    except Exception as e:
        return e
if __name__ == '__main__':
    app.run(host='0.0.0.0',port=5002)