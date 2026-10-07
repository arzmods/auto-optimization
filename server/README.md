# Your Minecraft 26.3 plugin server (Windows)

This is a **Paper** server. Paper is a normal Minecraft server that can run plugins:
you drop a plugin file into the `plugins` folder and it works.

You only have to do **one thing**: double-click `start.bat`.
The first time, it downloads everything by itself:

- **Java 25** (a private copy inside this folder, so you don't install anything)
- the **Paper server** for Minecraft 26.3
- **playit.gg**, so friends outside your Wi-Fi can join

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

## Add a plugin

1. Download a plugin `.jar` file. Good places: https://modrinth.com/plugins,
   https://hangar.papermc.io and https://www.spigotmc.org/resources/.
   Pick one that says it works with **Paper** (or Spigot/Bukkit) and Minecraft **26.3**.
2. Stop the server (type `stop`).
3. Drop the `.jar` file into the **`plugins`** folder.
4. Double-click `start.bat` again. The plugin is now running.

To check which plugins are loaded, type `plugins` in the black window.
To remove a plugin, stop the server and delete its `.jar` from `plugins`.

## Join the server

1. Open Minecraft **26.3** and click **Multiplayer**, then **Add Server**.
2. Server Address:
   - On the same PC as the server: `localhost`
   - Friends on the same Wi-Fi: your PC's local IP (it looks like `192.168.1.23`).
     `start.bat` shows it in green under **HOW TO JOIN** every time it starts.
3. Click **Done**, then click the server to join.

## Let friends anywhere join (playit.gg)

`start.bat` also opens a second window called **playit.gg**. It gives you an address
anyone can use, without touching your router. The setup below is **only needed once**:

1. In the playit window you'll see a link (it starts with `https://playit.gg/claim/...`).
   Hold **Ctrl** and click it, or copy it into your browser.
2. Make a free playit.gg account (or log in) and click to **add/approve** the agent.
3. On the playit.gg website, click **Add Tunnel**, choose **Minecraft Java**, and
   create it. (If it asks for a local port, use `25565`.)
4. The website shows your address, something like `cool-name.joinmc.link`.
   **Give that address to your friends.** It stays the same every time.

Keep the playit window open while you play. To turn playit off, open `start.bat`
with Notepad and change `set PLAYIT=yes` to `set PLAYIT=no`.

## Make yourself admin

In the black window, type `op YourMinecraftName` and press **Enter**.

## Change settings

- **Server name, players, difficulty and more:** open `server.properties` with Notepad,
  change it, save, and restart the server.
- **Memory:** open `start.bat` with Notepad and change `set RAM=4G` (for example to `6G`).
  Don't use more than half of your PC's memory.
- **Minecraft version:** change `set MC_VERSION=26.3` in `start.bat`.
  The next start downloads the matching Paper automatically.

## Something went wrong?

| Message | What to do |
|---|---|
| `Setup failed` | Usually the internet. Try again. |
| `Paper does not have Minecraft 26.3 yet` | Paper isn't out for 26.3 yet. Wait, or set `MC_VERSION=26.2` in `start.bat`. |
| A plugin doesn't work | Make sure that plugin supports Minecraft 26.3, and look for red errors in the black window. |

Still stuck? Copy the text from the black window and send it to me.
