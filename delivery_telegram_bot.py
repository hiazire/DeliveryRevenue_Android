# -*- coding: utf-8 -*-
"""
🛵 外送營業額專屬 Telegram Bot (RevenueReportAndroidbot) 🛵
功能：自動辨識外送平台截圖中的金額與時間並加總，寄送報告至指定信箱。
"""

import os
import sys
import json
import time
import base64
import html
import smtplib
import datetime
import urllib.request
import asyncio
from email.mime.text import MIMEText
from email.header import Header
from telegram import Bot, Update, InlineKeyboardButton, InlineKeyboardMarkup
from telegram.ext import ApplicationBuilder, ContextTypes, MessageHandler, CommandHandler, CallbackQueryHandler, filters

TOKEN = "8706811420:AAESlaREpTVmsrJxPTbr7zApl3Zfmx4Dv6E"
CHAT_ID = 6189047469

# 定義本地儲存路徑
BASE_DIR = "/Users/sufuhan/Documents/DeliveryRevenue"
SETTINGS_FILE = os.path.join(BASE_DIR, "delivery_settings.json")
DELIVERY_DATA_FILE = os.path.join(BASE_DIR, "delivery_today.json")
PHOTO_DIR = os.path.join(BASE_DIR, "delivery_photos")

# QBurger 專案中可能的環境變數路徑 (用作 Fallback API Key)
QBURGER_ENV_FILE = "/Users/sufuhan/Desktop/QBurger_POS/.env"

def private_only(func):
    async def wrapper(update: Update, context: ContextTypes.DEFAULT_TYPE, *args, **kwargs):
        chat_id = update.effective_chat.id if update.effective_chat else None
        if not chat_id or chat_id != CHAT_ID:
            return
        return await func(update, context, *args, **kwargs)
    return wrapper

def load_settings():
    default_settings = {
        "sender_email": "",
        "sender_password": "",
        "recipient_email": "",
        "smtp_host": "smtp.gmail.com",
        "smtp_port": 587,
        "gemini_api_key": ""
    }
    if os.path.exists(SETTINGS_FILE):
        try:
            with open(SETTINGS_FILE, "r", encoding="utf-8") as f:
                return {**default_settings, **json.load(f)}
        except Exception:
            pass
    return default_settings

def save_settings(settings):
    try:
        os.makedirs(BASE_DIR, exist_ok=True)
        with open(SETTINGS_FILE, "w", encoding="utf-8") as f:
            json.dump(settings, f, ensure_ascii=False, indent=2)
        return True
    except Exception as e:
        print(f"Save settings error: {e}")
        return False

def get_api_key(settings):
    # 優先使用本地 settings 中的 Key
    if settings.get("gemini_api_key"):
        return settings["gemini_api_key"]
    
    # Fallback 讀取 QBurger 的 .env
    if os.path.exists(QBURGER_ENV_FILE):
        try:
            with open(QBURGER_ENV_FILE, "r", encoding="utf-8") as f:
                content = f.read()
            for line in content.split("\n"):
                if line.startswith("GEMINI_API_KEY="):
                    return line.split("=", 1)[1].strip()
        except Exception:
            pass
    return ""

def load_delivery_data():
    if os.path.exists(DELIVERY_DATA_FILE):
        try:
            with open(DELIVERY_DATA_FILE, "r", encoding="utf-8") as f:
                return json.load(f)
        except Exception:
            pass
    return []

def save_delivery_data(data):
    try:
        os.makedirs(BASE_DIR, exist_ok=True)
        with open(DELIVERY_DATA_FILE, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    except Exception as e:
        print(f"Save delivery data error: {e}")

async def analyze_delivery_screenshot(image_path: str, api_key: str):
    if not api_key:
        return None
    try:
        loop = asyncio.get_event_loop()
        def read_and_encode():
            with open(image_path, "rb") as f:
                return base64.b64encode(f.read()).decode("utf-8")
        
        img_base64 = await loop.run_in_executor(None, read_and_encode)
        
        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
        
        prompt = (
            "你是一個外送平台帳單分析助手。\n"
            "請分析這張截圖是否為 FoodPanda 或 Uber Eats 的歷史訂單/交易明細截圖。\n"
            "如果是，請精確提取截圖中「每一筆」訂單的以下資訊：\n"
            "1. platform：平台名稱。只能是 \"FoodPanda\" 或 \"Uber Eats\"。\n"
            "2. time：訂單成立時間。格式為 24 小時制 \"HH:mm\" (例如 \"13:40\", \"09:15\" 等，如果原圖是 \"下午1:30\" 請轉為 \"13:30\"，\"上午10:15\" 轉為 \"10:15\")。\n"
            "3. amount：金額。數值（Double，若有小數點保留，通常為整數）。\n\n"
            "請以 JSON 格式回傳，格式如下：\n"
            "{\n"
            "  \"is_delivery_screenshot\": true,\n"
            "  \"orders\": [\n"
            "    {\"platform\": \"FoodPanda\", \"time\": \"11:30\", \"amount\": 120.0},\n"
            "    {\"platform\": \"Uber Eats\", \"time\": \"14:15\", \"amount\": 250.0}\n"
            "  ]\n"
            "}\n\n"
            "如果這張截圖「不是」外送平台的訂單或明細截圖，請回傳：\n"
            "{\n"
            "  \"is_delivery_screenshot\": false,\n"
            "  \"orders\": []\n"
            "}\n\n"
            "注意：請只回傳純 JSON 內容，不要包含任何 markdown 標記（如 ```json）或多餘的文字說明。"
        )
        
        payload = {
            "contents": [{
                "parts": [
                    {"text": prompt},
                    {
                        "inlineData": {
                            "mimeType": "image/jpeg",
                            "data": img_base64
                        }
                    }
                ]
            }]
        }
        
        data = json.dumps(payload).encode('utf-8')
        req = urllib.request.Request(
            url, 
            data=data, 
            headers={'Content-Type': 'application/json'},
            method="POST"
        )
        
        def fetch():
            with urllib.request.urlopen(req, timeout=30) as response:
                return response.read().decode('utf-8')
                
        res_body = await loop.run_in_executor(None, fetch)
        res_json = json.loads(res_body)
        
        if 'candidates' in res_json and len(res_json['candidates']) > 0:
            text = res_json['candidates'][0]['content']['parts'][0]['text'].strip()
            if text.startswith("```"):
                text = text.split("```")[1]
                if text.startswith("json"):
                    text = text[4:]
            return json.loads(text.strip())
    except Exception as e:
        print(f"analyze_delivery_screenshot error: {e}")
    return None

def send_smtp_email(cfg, subject, body):
    sender = cfg.get("sender_email")
    password = cfg.get("sender_password")
    recipient = cfg.get("recipient_email")
    host = cfg.get("smtp_host", "smtp.gmail.com")
    port = int(cfg.get("smtp_port", 587))
    
    if not sender or not password or not recipient:
        return False, "❌ 設定未完全。請確保 sender_email, sender_password, recipient_email 皆已完成配置。"
        
    try:
        msg = MIMEText(body, 'plain', 'utf-8')
        msg['From'] = Header(sender, 'utf-8')
        msg['To'] = Header(recipient, 'utf-8')
        msg['Subject'] = Header(subject, 'utf-8')
        
        server = smtplib.SMTP(host, port)
        server.ehlo()
        server.starttls()
        server.ehlo()
        server.login(sender, password)
        server.sendmail(sender, [recipient], msg.as_string())
        server.close()
        return True, "✅ Email 報告發送成功！"
    except Exception as e:
        return False, f"❌ 發送信件失敗：{str(e)}"

@private_only
async def send_help(update: Update, context: ContextTypes.DEFAULT_TYPE):
    help_text = (
        "🛵 <b>外送營業額專屬助理</b> 🛵\n\n"
        "你可以使用以下指令與我互動：\n"
        "📊 /report - 依據 13:30 分野計算今日統計與寄信按鈕\n"
        "📋 /status - 查看目前已累計的暫存明細\n"
        "⚙️ /settings - 查看並設定您的發信信箱與 Key\n"
        "🗑️ /clear - 清空今日的所有暫存數據\n"
        "❓ /help - 顯示此說明選單\n\n"
        "📸 <b>直接傳送外送截圖給我</b>，我會自動辨識金額與時間並自動加總。"
    )
    await context.bot.send_message(chat_id=CHAT_ID, text=help_text, parse_mode="HTML")

@private_only
async def show_settings(update: Update, context: ContextTypes.DEFAULT_TYPE):
    cfg = load_settings()
    api_key = get_api_key(cfg)
    
    masked_pwd = "已設定 (已隱藏)" if cfg.get("sender_password") else "未設定"
    masked_key = f"{api_key[:6]}...{api_key[-4:]}" if api_key else "未偵測到"
    
    settings_text = (
        "⚙️ <b>當前發信與金鑰設定</b>\n\n"
        f"📩 收件者：<code>{cfg.get('recipient_email') or '未設定'}</code>\n"
        f"📤 寄件者：<code>{cfg.get('sender_email') or '未設定'}</code>\n"
        f"🔑 App密碼：<code>{masked_pwd}</code>\n"
        f"🌐 SMTP主機：<code>{cfg.get('smtp_host')}</code>\n"
        f"🔌 SMTP Port：<code>{cfg.get('smtp_port')}</code>\n"
        f"🤖 Gemini Key：<code>{masked_key}</code>\n\n"
        "<b>變更設定指令說明：</b>\n"
        "• <code>/set_recipient &lt;email&gt;</code> - 設定收件者信箱\n"
        "• <code>/set_sender &lt;email&gt;</code> - 設定寄件者 Gmail\n"
        "• <code>/set_password &lt;16碼密碼&gt;</code> - 設定 Gmail 應用程式密碼\n"
        "• <code>/set_key &lt;key&gt;</code> - 設定專屬 Gemini API Key"
    )
    await context.bot.send_message(chat_id=CHAT_ID, text=settings_text, parse_mode="HTML")

@private_only
async def set_config(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if not context.args:
        await update.message.reply_text("⚠️ 請輸入設定值。例如：/set_recipient example@gmail.com")
        return
        
    value = context.args[0].strip()
    cmd = update.message.text.split()[0].replace("/", "")
    
    cfg = load_settings()
    field_name = ""
    
    if cmd == "set_recipient":
        cfg["recipient_email"] = value
        field_name = "收件者信箱"
    elif cmd == "set_sender":
        cfg["sender_email"] = value
        field_name = "寄件者信箱"
    elif cmd == "set_password":
        cfg["sender_password"] = value
        field_name = "SMTP 密碼"
    elif cmd == "set_key":
        cfg["gemini_api_key"] = value
        field_name = "Gemini API Key"
        
    if field_name:
        if save_settings(cfg):
            await update.message.reply_text(f"✅ 已成功更新 <b>{field_name}</b> 為 <code>{value}</code>", parse_mode="HTML")
        else:
            await update.message.reply_text("❌ 設定儲存失敗，請檢查系統寫入權限。")

@private_only
async def clear_data_cmd(update: Update, context: ContextTypes.DEFAULT_TYPE):
    save_delivery_data([])
    await update.message.reply_text("🗑️ 今日外送營業額暫存數據已成功清除歸零。")

@private_only
async def show_status(update: Update, context: ContextTypes.DEFAULT_TYPE):
    orders = load_delivery_data()
    if not orders:
        await context.bot.send_message(chat_id=CHAT_ID, text="📋 目前沒有暫存的訂單數據。請先上傳外送明細截圖！")
        return
        
    lines = []
    for idx, o in enumerate(orders, 1):
        lines.append(f"{idx}. [{o.get('platform')}] {o.get('time')} - NT$ {o.get('amount'):.0f}")
        
    status_text = (
        "📋 <b>今日暫存外送訂單明細</b>\n\n" + 
        "\n".join(lines) + 
        f"\n\n總計共 <b>{len(orders)}</b> 筆訂單。"
    )
    await context.bot.send_message(chat_id=CHAT_ID, text=status_text, parse_mode="HTML")

@private_only
async def show_report(update: Update, context: ContextTypes.DEFAULT_TYPE):
    orders = load_delivery_data()
    
    normal_panda = 0.0
    normal_ue = 0.0
    extended_panda = 0.0
    extended_ue = 0.0
    
    for o in orders:
        platform = o.get("platform", "Unknown").lower()
        t_str = o.get("time", "12:00")
        amount = o.get("amount", 0.0)
        
        is_extended = t_str >= "13:30"
        
        if "panda" in platform:
            if is_extended:
                extended_panda += amount
            else:
                normal_panda += amount
        elif "eats" in platform:
            if is_extended:
                extended_ue += amount
            else:
                normal_ue += amount
                
    total_amt = normal_panda + normal_ue + extended_panda + extended_ue
    
    normal_total = normal_panda + normal_ue
    extended_total = extended_panda + extended_ue
    
    report = (
        f"🛵 <b>[外送營業額今日報告]</b>\n\n"
        f"<b>【一般營業時間】</b>\n"
        f"FoodPanda：{normal_panda:.0f}\n"
        f"Uber Eats：{normal_ue:.0f}\n"
        f"一般營時 + 雙平台 Total：{normal_total:.0f}\n\n"
        f"<b>【延長營業時間】</b>\n"
        f"FoodPanda：{extended_panda:.0f}\n"
        f"Uber Eats：{extended_ue:.0f}\n"
        f"延長營時 + 雙平台 Total：{extended_total:.0f}\n\n"
        f"外送平台總金額：<b>{total_amt:.0f}</b> 元\n"
        f"總交易筆數：<b>{len(orders)}</b> 筆"
    )
    
    keyboard = InlineKeyboardMarkup([
        [
            InlineKeyboardButton("✅ 寄送 Email 報告", callback_data="send_delivery_email"),
            InlineKeyboardButton("🗑️ 清除今日數據", callback_data="clear_delivery_data")
        ]
    ])
    
    await context.bot.send_message(
        chat_id=CHAT_ID,
        text=report,
        parse_mode="HTML",
        reply_markup=keyboard
    )

@private_only
async def handle_photo(update: Update, context: ContextTypes.DEFAULT_TYPE):
    if update.message and update.message.photo:
        await context.bot.send_chat_action(chat_id=CHAT_ID, action="upload_photo")
        
        photo_file = update.message.photo[-1]
        file_id = photo_file.file_id
        
        os.makedirs(PHOTO_DIR, exist_ok=True)
        filename = f"delivery_{int(time.time())}.jpg"
        filepath = os.path.join(PHOTO_DIR, filename)
        
        try:
            new_file = await context.bot.get_file(file_id)
            await new_file.download_to_drive(filepath)
            
            cfg = load_settings()
            api_key = get_api_key(cfg)
            
            if not api_key:
                await context.bot.send_message(
                    chat_id=CHAT_ID,
                    text="⚠️ 偵測到上傳照片，但尚未設定 Gemini API Key。請先使用 <code>/set_key</code> 設定，或配置本機環境變數。" ,
                    parse_mode="HTML"
                )
                return
                
            await context.bot.send_message(chat_id=CHAT_ID, text="🔍 正在利用 AI 分析外送截圖...")
            await context.bot.send_chat_action(chat_id=CHAT_ID, action="typing")
            
            res = await analyze_delivery_screenshot(filepath, api_key)
            if res and res.get("is_delivery_screenshot"):
                orders = res.get("orders", [])
                today_data = load_delivery_data()
                today_data.extend(orders)
                save_delivery_data(today_data)
                
                orders_count = len(orders)
                total_amt = sum(o.get("amount", 0.0) for o in orders)
                
                await context.bot.send_message(
                    chat_id=CHAT_ID,
                    text=(
                        f"🛵 <b>[外送營業額助理]</b> 辨識成功！\n\n"
                        f"從此截圖成功讀取 <b>{orders_count}</b> 筆交易，合計 <b>NT$ {total_amt:.0f}</b> 元。\n\n"
                        f"💡 <i>輸入 /report 以查看總和報表與發送郵件。</i>"
                    ),
                    parse_mode="HTML"
                )
            else:
                await context.bot.send_message(
                    chat_id=CHAT_ID,
                    text="❌ 這張圖片似乎不是外送平台明細截圖，或未能識別出有效的金額/平台。"
                )
        except Exception as e:
            await context.bot.send_message(chat_id=CHAT_ID, text=f"❌ 圖片處理失敗: {str(e)}")

@private_only
async def button_callback_handler(update: Update, context: ContextTypes.DEFAULT_TYPE):
    query = update.callback_query
    try:
        await query.answer()
    except Exception:
        pass
        
    data = query.data
    
    if data == "send_delivery_email":
        cfg = load_settings()
        orders = load_delivery_data()
        
        normal_panda = 0.0
        normal_ue = 0.0
        extended_panda = 0.0
        extended_ue = 0.0
        
        for o in orders:
            platform = o.get("platform", "Unknown").lower()
            t_str = o.get("time", "12:00")
            amount = o.get("amount", 0.0)
            is_extended = t_str >= "13:30"
            
            if "panda" in platform:
                if is_extended:
                    extended_panda += amount
                else:
                    normal_panda += amount
            elif "eats" in platform:
                if is_extended:
                    extended_ue += amount
                else:
                    normal_ue += amount
                    
        total_amt = normal_panda + normal_ue + extended_panda + extended_ue
        
        normal_total = normal_panda + normal_ue
        extended_total = extended_panda + extended_ue
        
        today_str = datetime.date.today().strftime("%Y/%m/%d")
        subject = f"q8js_外送未入機加總金額 {today_str}"
        body = (
            f"【一般營業時間】\n"
            f"FoodPanda：{normal_panda:.0f}\n"
            f"Uber Eats：{normal_ue:.0f}\n"
            f"一般營時 + 雙平台 Total：{normal_total:.0f}\n\n"
            f"【延長營業時間】\n"
            f"FoodPanda：{extended_panda:.0f}\n"
            f"Uber Eats：{extended_ue:.0f}\n"
            f"延長營時 + 雙平台 Total：{extended_total:.0f}\n\n"
            f"外送平台總金額：{total_amt:.0f}"
        )
        
        success, msg = send_smtp_email(cfg, subject, body)
        original_text = query.message.text
        
        new_text = f"{original_text}\n\n<b>{msg}</b>"
        await context.bot.edit_message_text(chat_id=CHAT_ID, message_id=query.message.message_id, text=new_text, parse_mode="HTML")
        
    elif data == "clear_delivery_data":
        save_delivery_data([])
        original_text = query.message.text
        new_text = f"{original_text}\n\n🗑️ <b>[今日暫存數據已清除歸零]</b>"
        await context.bot.edit_message_text(chat_id=CHAT_ID, message_id=query.message.message_id, text=new_text, parse_mode="HTML")

@private_only
async def handle_text_fallback(update: Update, context: ContextTypes.DEFAULT_TYPE):
    user_text = update.message.text.strip()
    if user_text in ["外送", "營業額", "計算", "計算外送"]:
        await show_report(update, context)
    else:
        await context.bot.send_message(
            chat_id=CHAT_ID,
            text="💡 <b>外送營業額助理提示</b>\n\n上傳外送截圖可以自動辨識，或者可以使用以下指令：\n• /report - 產生加總報表\n• /status - 查看累計明細\n• /settings - 信箱與 Key 設定",
            parse_mode="HTML"
        )

if __name__ == "__main__":
    from telegram.request import HTTPXRequest
    request = HTTPXRequest(connect_timeout=30.0, read_timeout=30.0)
    app = ApplicationBuilder().token(TOKEN).request(request).build()
    
    app.add_handler(CommandHandler("help", send_help))
    app.add_handler(CommandHandler("start", send_help))
    app.add_handler(CommandHandler("settings", show_settings))
    app.add_handler(CommandHandler("report", show_report))
    app.add_handler(CommandHandler("status", show_status))
    app.add_handler(CommandHandler("clear", clear_data_cmd))
    
    # 設定變更 handlers
    app.add_handler(CommandHandler("set_recipient", set_config))
    app.add_handler(CommandHandler("set_sender", set_config))
    app.add_handler(CommandHandler("set_password", set_config))
    app.add_handler(CommandHandler("set_key", set_config))
    
    app.add_handler(CallbackQueryHandler(button_callback_handler))
    app.add_handler(MessageHandler(filters.PHOTO, handle_photo))
    app.add_handler(MessageHandler(filters.TEXT & ~filters.COMMAND, handle_text_fallback))
    
    print("🤖 外送營業額專屬 Telegram Bot 已啟動，正在背景監聽中...")
    app.run_polling()
