from telethon import TelegramClient, sessions
import asyncio
import os
import sys


def require_env(name):
    value = os.environ.get(name)
    if not value:
        raise SystemExit(f"[-] Missing required env: {name}")
    return value


API_ID = int(require_env("API_ID"))
API_HASH = require_env("API_HASH")
BOT_TOKEN = require_env("BOT_TOKEN")
CHAT_ID = int(require_env("CHAT_ID"))
BOT_CI_SESSION = require_env("BOT_CI_SESSION")


async def send_telegram_files(files, caption=None):
    """
    Connects to Telegram and sends the specified files.
    """
    missing = [path for path in files if not os.path.isfile(path)]
    if missing:
        raise FileNotFoundError(f"File(s) not found: {missing}")

    session = sessions.StringSession(BOT_CI_SESSION)

    async with TelegramClient(session, api_id=API_ID, api_hash=API_HASH) as client:
        await client.start(bot_token=BOT_TOKEN)

        print("[+] Sending file(s)...")
        # 单文件直接传路径；多文件传列表。Telethon 会按路径上传。
        await client.send_file(
            entity=CHAT_ID,
            file=files if len(files) > 1 else files[0],
            caption=caption,
            parse_mode="html",
            force_document=True,
        )
        print("[+] File(s) sent successfully.")


if __name__ == "__main__":
    caption = None
    files = []

    args = sys.argv[1:]
    i = 0
    while i < len(args):
        if args[i] == "--caption":
            i += 1
            if i < len(args):
                caption = args[i]
            else:
                raise SystemExit("[-] --caption requires a value")
        else:
            files.append(args[i])
        i += 1

    if not files:
        raise SystemExit("[-] No file paths provided as arguments.")

    print(f"[+] Found files to upload: {files}")
    print(f"[+] Caption: {caption}")
    try:
        asyncio.run(send_telegram_files(files, caption=caption))
    except Exception as e:
        print(f"[-] An error occurred: {e}")
        raise SystemExit(1) from e
