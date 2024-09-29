#!/usr/bin/python

import asyncio
import datetime
import websockets
import json
import logging

USER = USERNAME

# Configure logging
logging.basicConfig(filename='/home/USER/websocket.log', level=logging.INFO)

# Set to store connected clients
connected_clients = set()

async def handle_client(websocket, path):
    connected_clients.add(websocket)
    now = datetime.datetime.now()
    print(f"New client connected at {now.strftime('%d-%m-%Y %H:%M:%S')}")
    logging.info(f"New client connected at {now.strftime('%d-%m-%Y %H:%M:%S')}")

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
                # Forward the message to all connected clients (including the sender)
                for client in connected_clients:
                    await client.send(json.dumps({
                        "timestamp": now.strftime('%d-%m-%Y %H:%M:%S'),
                        **message_data
                    }))
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
    server = await websockets.serve(handle_client, "0.0.0.0", 6789)
    print("WebSocket server is running")
    logging.info("WebSocket server is running")
    await asyncio.Future()  # Run forever

if __name__ == "__main__":
    asyncio.run(main())