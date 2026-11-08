import request from "./request";

const botApi = {
    createBot(){
        return request.get('/createBot')
    }
}

export default botApi
