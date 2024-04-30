from flask import Flask,request, send_from_directory
import sqlite3
app = Flask(__name__)

conn = sqlite3.connect('../db/mapdata.db', check_same_thread=False)
c = conn.cursor()

# Create a table if it doesn't exist
c.execute('''CREATE TABLE IF NOT EXISTS requests
             (id INTEGER PRIMARY KEY AUTOINCREMENT, data TEXT)''')
conn.commit()

@app.route('/save_request', methods=['POST'])
def save_request():
    data = request.json.get('data')
    if data:
        # Save the request data to the database
        c.execute("INSERT INTO requests (data) VALUES (?)", (data,))
        conn.commit()
        return 'Request saved successfully'
    else:
        return 'No data provided'

@app.route('/get_requests')
def get_requests():
    # Retrieve all requests from the database
    c.execute("SELECT * FROM requests")
    rows = c.fetchall()
    return {'requests': rows}

@app.route('/resources/<filename>')
def download_file(filename):
    try:
        return send_from_directory('resources', filename)
    except Exception as e:
        return e
if __name__ == '__main__':
    app.run(host='0.0.0.0',port=5002)