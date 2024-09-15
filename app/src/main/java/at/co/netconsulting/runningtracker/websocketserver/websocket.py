#!/usr/bin/python

import asyncio
import websockets
import json
import datetime

# Dictionary to store connected clients
connected_clients = set()

async def handle_client(websocket, path):
    connected_clients.add(websocket)
    now = datetime.datetime.now()
    #print(f"New client connected: {websocket.remote_address}")
    print(f"New client connected at {now.strftime("%d-%m-%Y %H:%M:%S")}")
    try:
        async for message in websocket:
            now = datetime.datetime.now()
            print(f"Received message at date and time: {now.strftime("%d-%m-%Y %H:%M:%S")}")
            print(f"Received message is: {message}")
            for client in connected_clients:
                if client != websocket:
                    await client.send(message)
    except websockets.exceptions.ConnectionClosed as e:
        print(f"Client disconnected: {websocket.remote_address}")

    finally:
        # Unregister the client
        connected_clients.remove(websocket)

async def main():
    server = await websockets.serve(handle_client, "YOUR_IP_ADDRESS", WEBSOCKET_PORT)
    print("Websocket server is running")
    await asyncio.Future()  # Run forever

if __name__ == "__main__":
    asyncio.run(main())