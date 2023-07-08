import json

# Get token from https://developers.google.com/oauthplayground/
# WARNING: DO NOT USE YOUR PERSONAL GOOGLE ACCOUNT: RESTTESTGEN WILL ERASE YOUR DATA IN GOOGLE DRIVE!!!

token = "your_token_here"

rtg_info = {
    "name": "Authorization",
    "value": "Bearer " + token,
    "in": "header",
    "duration": 600
}

print(json.dumps(rtg_info))