import os
import json
import requests

def time_to_float_hhmm(t_str):
    h, m = t_str.split(':')
    return float(f"{int(h)}.{int(m)}")

def process_location(latitude, longitude, time, day, save_path):
    try:
        time_float = time_to_float_hhmm(time)

        day_to_num = {
            'Monday': 1, 'Tuesday': 2, 'Wednesday': 3,
            'Thursday': 4, 'Friday': 5, 'Saturday': 6, 'Sunday': 7
        }
        day_num = day_to_num.get(day, None)
        if day_num is None:
            return "❌ Invalid day format"

        processed_data = {
            "latitude": float(latitude),
            "longitude": float(longitude),
            "time_minutes": time_float,
            "day": day_num
        }

        # Send to backend
        url = 'http://192.168.170.155:5000/add-new'
        payload = {"processed_data": processed_data}
        response = requests.post(url, json=payload)

        if response.status_code != 200:
            return f"❌ Failed to send data: {response.status_code} - {response.text}"

        server_response = response.json()
        cluster_id = server_response.get("last_added_row", {}).get("cluster", None)

        if cluster_id is None:
            return "⚠️ Could not retrieve cluster ID from response."

        # Step 2: Fetch summary
        summary_status = fetch_summary_and_save(save_path)
        if "❌" in summary_status:
            return summary_status

        # Step 3: Check if this time fits usual behavior
        file_path = os.path.join(save_path, 'summary_data.json')
        with open(file_path, 'r') as f:
            summary_data = json.load(f)

        is_usual = False
        for cluster in summary_data:
            if cluster["cluster_id"] == cluster_id:
                if day_num in cluster.get("active_days", []):
                    start = cluster.get("start_time", 0)
                    end = cluster.get("end_time", 0)
                    if start <= time_float <= end:
                        is_usual = True
                break

        if is_usual:
            return f"✅ Usual behavior (Cluster: {cluster_id})"
        else:
            return f"alert"

    except Exception as e:
        return f"❌ Error during logic: {str(e)}"


def fetch_summary_and_save(save_path):
    url = 'http://192.168.170.155:5000/generate-summary'
    try:
        response = requests.get(url)
        if response.status_code == 200:
            data = response.json()
            file_path = os.path.join(save_path, 'summary_data.json')
            with open(file_path, 'w') as f:
                json.dump(data, f, indent=4)
            return "✅ Summary saved successfully"
        else:
            return f"❌ Error: status code {response.status_code}"
    except Exception as e:
        return f"❌ Error: {str(e)}"


import requests  # make sure you import the correct library

def checkbpm():
    url = 'https://wsafe-bc890-default-rtdb.asia-southeast1.firebasedatabase.app/bpm.json'  # include `.json` at the end

    try:
        response = requests.get(url)  # use `requests` not `request`
        if response.status_code == 200:
            data = response.json()
            
            # Convert the data to an integer
            bpm_value = int(data) if isinstance(data, (str, int, float)) else 0
            
            if bpm_value >= 30:
                return "high bpm"
            else:
                return "normal"
        else:
            return f"❌ HTTP Error: {response.status_code}"
        
    except Exception as e:
        return f"❌ Error: {str(e)}"

print(checkbpm())