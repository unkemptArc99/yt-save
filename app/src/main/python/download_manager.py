import json
import os
import traceback
import subprocess

# MOCK subprocess.Popen as a CLASS to prevent Chaquopy SIGSEGV 
# and to satisfy yt-dlp which inherits from it via class Popen(subprocess.Popen):
_original_popen = subprocess.Popen

class SafePopen(_original_popen):
    def __init__(self, *args, **kwargs):
        # Do NOT call super().__init__ because that will trigger the real Popen and SIGSEGV
        self.args = args
        self.stdin = None
        self.stdout = None
        self.stderr = None
        self.returncode = 1
        self.pid = 99999
        
    def communicate(self, input=None, timeout=None):
        return (b"", b"")
        
    def wait(self, timeout=None):
        return 1
        
    def kill(self):
        pass
        
    def terminate(self):
        pass

subprocess.Popen = SafePopen

import yt_dlp

def create_progress_hook(callback):
    def _progress_hook(d):
        if callback is None:
            return

        try:
            downloaded = d.get("downloaded_bytes", 0)
            total = d.get("total_bytes") or d.get("total_bytes_estimate", 0)
            speed = d.get("speed", 0) or 0
            eta = d.get("eta", 0) or 0

            progress = 0
            if total and total > 0:
                progress = int((downloaded / total) * 100)

            info = {
                "status": d.get("status", "unknown"),
                "downloaded_bytes": downloaded,
                "total_bytes": total,
                "speed": float(speed),
                "eta": int(eta),
                "filename": d.get("filename", ""),
                "progress": min(progress, 100),
            }
            callback.call(json.dumps(info))
        except Exception:
            pass
    return _progress_hook

def extract_info(url):
    ydl_opts = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
        "format": "best/bestvideo/bestaudio",
        "extractor_args": {"youtube": ["client=ANDROID,WEB"]},
    }
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=False)
            
            formats = []
            for f in info.get("formats", []):
                if not f.get("format_id"):
                    continue
                    
                height = f.get("height", 0) or 0
                
                if height > 0:
                    resolution = f"{height}p"
                elif f.get("acodec", "none") != "none" and f.get("vcodec", "none") == "none":
                    abr = f.get("abr", 0) or 0
                    resolution = f"audio {int(abr)}kbps" if abr else "audio"
                else:
                    resolution = f.get("resolution", "unknown")
                
                formats.append({
                    "format_id": str(f.get("format_id")),
                    "ext": str(f.get("ext", "")),
                    "resolution": resolution,
                    "filesize": f.get("filesize") or f.get("filesize_approx"),
                    "vcodec": str(f.get("vcodec", "none")),
                    "acodec": str(f.get("acodec", "none")),
                    "height": int(height),
                    "abr": float(f.get("abr", 0) or 0),
                })
            
            result = {
                "title": str(info.get("title", "Unknown")),
                "channel": str(info.get("channel") or info.get("uploader", "Unknown")),
                "thumbnail": str(info.get("thumbnail", "")),
                "duration": int(info.get("duration", 0) or 0),
                "formats": formats,
            }
            return json.dumps(result)
    except Exception as e:
        import traceback
        traceback.print_exc()
        return json.dumps({
            "error": str(e),
            "title": "",
            "channel": "",
            "thumbnail": "",
            "duration": 0,
            "formats": [],
        })

def download_video(url, output_dir, format_spec="bestvideo+bestaudio/best", callback=None):
    os.makedirs(output_dir, exist_ok=True)
    
    ydl_opts = {
        "format": format_spec,
        "outtmpl": os.path.join(output_dir, "%(title)s.%(ext)s"),
        "progress_hooks": [create_progress_hook(callback)] if callback else [],
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "overwrites": True,
        "restrictfilenames": False,
        "windowsfilenames": True,
        "extractor_args": {"youtube": ["client=ANDROID,WEB"]},
    }
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            filename = ydl.prepare_filename(info)
            
            actual_file = filename
            if not os.path.exists(filename):
                base = os.path.splitext(filename)[0]
                for ext in [".mp4", ".webm", ".mkv", ".m4a", ".mp3", ".opus"]:
                    candidate = base + ext
                    if os.path.exists(candidate):
                        actual_file = candidate
                        break
            
            filesize = os.path.getsize(actual_file) if os.path.exists(actual_file) else 0
            
            return json.dumps({
                "success": True,
                "filename": actual_file,
                "title": str(info.get("title", "Unknown")),
                "channel": str(info.get("channel") or info.get("uploader", "Unknown")),
                "thumbnail": str(info.get("thumbnail", "")),
                "duration": int(info.get("duration", 0) or 0),
                "filesize": filesize,
            })
    except Exception as e:
        return json.dumps({
            "success": False,
            "filename": "",
            "title": "",
            "channel": "",
            "thumbnail": "",
            "duration": 0,
            "filesize": 0,
            "error": str(e),
        })

def download_audio_only(url, output_dir, audio_format="m4a", callback=None):
    os.makedirs(output_dir, exist_ok=True)
    
    ydl_opts = {
        "format": "bestaudio/best",
        "outtmpl": os.path.join(output_dir, "%(title)s.%(ext)s"),
        "progress_hooks": [create_progress_hook(callback)] if callback else [],
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "overwrites": True,
        "windowsfilenames": True,
        "extractor_args": {"youtube": ["client=ANDROID,WEB"]},
        "postprocessors": [{
            "key": "FFmpegExtractAudio",
            "preferredcodec": audio_format,
            "preferredquality": "192",
        }],
    }
    
    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            filename = ydl.prepare_filename(info)
            
            base = os.path.splitext(filename)[0]
            final_filename = f"{base}.{audio_format}"
            
            if not os.path.exists(final_filename):
                final_filename = filename
            
            filesize = os.path.getsize(final_filename) if os.path.exists(final_filename) else 0
            
            return json.dumps({
                "success": True,
                "filename": final_filename,
                "title": str(info.get("title", "Unknown")),
                "channel": str(info.get("channel") or info.get("uploader", "Unknown")),
                "thumbnail": str(info.get("thumbnail", "")),
                "duration": int(info.get("duration", 0) or 0),
                "filesize": filesize,
            })
    except Exception as e:
        return json.dumps({
            "success": False,
            "filename": "",
            "title": "",
            "channel": "",
            "thumbnail": "",
            "duration": 0,
            "filesize": 0,
            "error": str(e),
        })
