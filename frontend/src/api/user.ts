import request from "./request";

export interface LoginForm {
    username: string;
    password: string;
}
export interface RegisterForm{
    username: string;
    password: string;
    repassword: string;
}
export interface LibraryForm {
    title: string;
    description: string;
    content: string;
}
const userApi = {
    login(form: LoginForm) {
        return request.post("/login", form)
    },
    register(form:RegisterForm){
        return request.post("/register",form)
    },
    getUserInfo(){
        return request.get("/userInfo")
    },
    getAiSetting(){
        return request.get("/getAiSetting")
    },
    setPersona(persona: string){
        return request.put("/setPersona", { persona })
    },
    getUsage(){
        return request.get("/getUsage")
    },
    getLibrary(){
        return request.get("/getLibrary")
    },
    addLibrary(form: LibraryForm){
        return request.post("/addLibrary", form)
    },
    updateLibrary(form: LibraryForm & { id: number }){
        return request.put("/updateLibrary", form)
    },
    deleteLibrary(id: number){
        return request.delete(`/deleteLibrary/${id}`)
    }
}

export default userApi
