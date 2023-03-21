from fastapi import FastAPI

app = FastAPI()


@app.get("/")
def get_root() -> dict[str, str]:
    return {"hello": "world"}
