# Your Minecraft 26.3 Fabric server (Windows)

You only have to do **one thing**: double-click `start.bat`.
The first time, it downloads everything by itself:

- **Java 25** (a private copy inside this folder, so you don't install anything)
- the **Fabric server** for Minecraft 26.3
- the **Fabric API** mod, which most Fabric mods need

## Start the server

1. Copy this whole `server` folder somewhere easy, like `C:\MinecraftServer`.
2. Double-click **`start.bat`**.
   - If Windows says "Windows protected your PC", click **More info** and then **Run anyway**.
3. Wait while it downloads (a few minutes the first time).
4. It asks you to agree to the Minecraft EULA (https://aka.ms/MinecraftEULA).
   Type `YES` and press **Enter**.
5. If **Windows Firewall** pops up, click **Allow access**.
6. When you see a line with **`Done`**, the server is running.

To stop it: type `stop` in the black window and press **Enter**.

## Join the server

1. Open Minecraft **26.3** and click **Multiplayer**, then **Add Server**.
2. Server Address:
   - On the same PC as the server: `localhost`
   - Friends on the same Wi-Fi: your PC's local IP. To find it, press the Windows key,
     type `cmd`, press Enter, type `ipconfig`, press Enter, and copy the **IPv4 Address**
     (it looks like `192.168.1.23`).
3. Click **Done**, then click the server to join.

Friends who are **not** on your Wi-Fi need "port forwarding" (port `25565`) on your
router, or a tool like playit.gg. Ask me if you want help with that.

## Add mods

1. Stop the server.
2. Put the mod `.jar` files in the **`mods`** folder (pick the 26.3 Fabric version of each mod).
3. Start the server again.

Only server mods do anything here. "Client only" mods, like the Hardware Scaler
mod in this project, go in each player's own game instead.

## Change settings

- **Server name, players, difficulty and more:** open `server.properties` with Notepad,
  change it, save, and restart the server.
- **Memory:** open `start.bat` with Notepad and change `set RAM=4G` (for example to `6G`).
  Don't use more than half of your PC's memory.
- **Minecraft version:** change `set MC_VERSION=26.3` in `start.bat`.
  The next start updates Fabric automatically.

## Something went wrong?

| Message | What to do |
|---|---|
| `Setup failed` | Usually the internet. Try again. |
| `Fabric does not support Minecraft 26.3 yet` | Fabric isn't out for 26.3 yet. Wait, or set `MC_VERSION=26.2` in `start.bat`. |
| The window closes right away | Right-click `start.bat`, choose **Edit**, and check that you didn't break a line. |

Still stuck? Copy the text from the black window and send it to me.
