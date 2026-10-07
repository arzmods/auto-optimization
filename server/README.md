# How to run your Minecraft Fabric server (Windows)

This folder has everything you need to start a **Minecraft 26.2 Fabric server**
on your own Windows PC. Follow the steps in order. It's your first time? That's fine!

## Step 1: Install Java 25

Minecraft 26.2 needs Java 25 or newer.

1. Go to **https://adoptium.net/**.
2. Download the **Temurin 25** installer for Windows (the `.msi` file).
3. Open it and click **Next** until it finishes.
   On the screen with features, make sure **"Set JAVA_HOME"** and **"Add to PATH"** are turned on.

## Step 2: Copy this folder to your PC

Copy the whole `server` folder somewhere easy to find, like `C:\MinecraftServer`.

## Step 3: Start the server

1. Double-click **`start.bat`**.
2. The first time, it will download the Fabric server files (this takes a minute).
3. It will ask you to agree to the Minecraft EULA. Read it at
   https://aka.ms/MinecraftEULA, then type `YES` and press **Enter**.
4. Wait until you see a line that says **`Done`**. Your server is now running!

To stop it, type `stop` in the black window and press **Enter**.
(Don't just close the window, or you might lose the last few minutes of your world.)

## Step 4: Join your server

1. Open Minecraft (version **26.2**).
2. Click **Multiplayer** -> **Add Server**.
3. For **Server Address** type `localhost` and click **Done**.
4. Click your server to join.

Friends on the **same Wi-Fi** can join with your PC's local IP address instead
of `localhost`. To find it: press the Windows key, type `cmd`, press Enter,
type `ipconfig` and press Enter, and look for **IPv4 Address** (looks like `192.168.1.23`).

## Adding mods

1. Stop the server.
2. Put the mod `.jar` files in the **`mods`** folder.
   Most mods also need **Fabric API**: download it from
   https://modrinth.com/mod/fabric-api (pick the 26.2 version).
3. Start the server again with `start.bat`.

Only use mods that say they work on a **server**. Mods that are "client only" do
nothing on a server.

## Changing settings

- **Memory:** open `start.bat` with Notepad and change `set RAM=4G`
  (for example to `6G`). Don't use more than half of your PC's memory.
- **Game settings** (name, game mode, difficulty, max players...): after the first
  start, open `server.properties` with Notepad, change what you want, save,
  and restart the server.

## Something went wrong?

| Message | What to do |
|---|---|
| `Java was not found` | Do Step 1 again, then restart your PC. |
| `UnsupportedClassVersionError` | Your Java is too old. Install Java 25 (Step 1). |
| `The download failed` | Check your internet and try again. |
| Windows Firewall pops up | Click **Allow access** so friends can connect. |
