import request from "./request";

const botApi = {
    createBot(){
        return request.get('/createBot')
    },
    connectTest(){
        return request.get('/connectTest')
    },
    clearContext(){
        return request.post('/clearContext')
    },
    unbindBot(){
        return request.post('/unbindBot')
    }
}

export default botApi
