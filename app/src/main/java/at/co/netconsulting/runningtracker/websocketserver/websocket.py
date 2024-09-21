#!/usr/bin/python

import asyncio
import datetime
import websockets
import json
import logging

# Configure logging
logging.basicConfig(filename='/home/bernd/websocket.log', level=logging.INFO)

# Set to store connected clients
connected_clients = set()

# List to store messages with timestamps, format: [(message_dict, timestamp), ...]
message_history = []

# Function to remove messages older than 24 hours
def cleanup_old_messages():
    now = datetime.datetime.now()
    # Keep only messages within the last 24 hours
    message_history[:] = [(msg, timestamp) for msg, timestamp in message_history
                          if now - timestamp < datetime.timedelta(hours=24)]

async def handle_client(websocket, path):
    connected_clients.add(websocket)
    now = datetime.datetime.now()
    print(f"New client connected at {now.strftime('%d-%m-%Y %H:%M:%S')}")
    logging.info(f"New client connected at {now.strftime('%d-%m-%Y %H:%M:%S')}")

    # On connection, send the last 24 hours' messages to the new client
    cleanup_old_messages()  # Clean up old messages before sending
    for msg, timestamp in message_history:
        # Send each message as JSON string with timestamp
        await websocket.send(json.dumps({"timestamp": timestamp.strftime('%d-%m-%Y %H:%M:%S'), **msg}))

    try:
        async for message in websocket:
            now = datetime.datetime.now()
            print(f"Received message at {now.strftime('%d-%m-%Y %H:%M:%S')}")
            print(f"Received message is: {message}")
            logging.info(f"Received message at {now.strftime('%d-%m-%Y %H:%M:%S')}")
            logging.info(f"Received message is: {message}")

            # Parse the received message as JSON
            message_data = json.loads(message)

            # Check if the message contains a sessionId, lat, lng (valid data)
            if all(key in message_data for key in ("person", "sessionId", "latitude", "longitude", "distance", "currentSpeed")):
                # Save the message with the timestamp
                message_history.append((message_data, now))

                # Forward the message to all connected clients (including the sender)
                for client in connected_clients:
                    await client.send(json.dumps({
                        "timestamp": now.strftime('%d-%m-%Y %H:%M:%S'),
                        **message_data
                    }))

                # Periodically clean up old messages
                cleanup_old_messages()
            else:
                print("Invalid message format. Missing required fields.")
                logging.info("Invalid message format. Missing required fields.")
    except websockets.exceptions.ConnectionClosed as e:
        print(f"Client disconnected: {websocket.remote_address}")
        logging.info(f"Client disconnected: {websocket.remote_address}")

    finally:
        # Unregister the client
        connected_clients.remove(websocket)

async def main():
    server = await websockets.serve(handle_client, "0.0.0.0", MY_PORT)
    print("WebSocket server is running")
    logging.info("WebSocket server is running")
    await asyncio.Future()  # Run forever

if __name__ == "__main__":
    asyncio.run(main())