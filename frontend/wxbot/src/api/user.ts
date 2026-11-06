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
const userApi = {
    login(form: LoginForm) {
        return request.post("/login", form)
    },
    register(form:RegisterForm){
        return request.post("/register",form)
    }
}

export default userApi
