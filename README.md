# pcp

Pixel Code Platform  
Self-hostable coding courses/problems platform

---

## Frontends

- [PixelSam123/pcp_frontend](https://github.com/PixelSam123/pcp_frontend)

## Deployment guide

You need to run the app with these environment variables, either from the command line or a `.env` file.

- `SECRET_KEY`: a secret generated with `openssl rand -hex 32`

Start the app with this command:

```
python -m uvicorn src.main:app --host 0.0.0.0 --forwarded-allow-ips="*"
```

It is recommended to change `*` with your proxy IP.

## Todos

- [ ] Remove unneeded user group join for some user data queries
