import axios from 'axios';

const baseURL = 'https://localhost:8080';

export const api = axios.create({
    baseURL
});
