const TK = "auth.token";
const US = "auth.user";
const EX = "auth.expiresAt";

function notifyAuthChanged() {
  window.dispatchEvent(new Event("auth:changed"));
}
function clearStorage() {
  localStorage.removeItem(TK);
  localStorage.removeItem(US);
  localStorage.removeItem(EX);
}

export function saveAuth({ token, user, expiresInSec }) {
  const exp = Date.now() + (expiresInSec ?? 7200) * 1000;
  localStorage.setItem(TK, token);
  localStorage.setItem(US, JSON.stringify(user));
  localStorage.setItem(EX, String(exp));
  notifyAuthChanged();                      
  return { token, user, exp };
}

export function clearAuth() {               
  clearStorage();
  notifyAuthChanged();                      
}

export function getToken() {
  const token = localStorage.getItem(TK);
  if (!token) return null;                  

  const exp = Number(localStorage.getItem(EX) || 0);
  if (!exp || Date.now() > exp) {
    clearStorage();                         
    return null;
  }
  return token;
}

export function getUser() {
  const t = getToken();
  if (!t) return null;
  try { return JSON.parse(localStorage.getItem(US) || "null"); }
  catch { return null; }
}

export function getAuth() {
  const token = getToken();
  if (!token) return null;
  const user = getUser();
  const exp = Number(localStorage.getItem(EX) || 0);
  return { token, user, exp };
}

export function isLoggedIn() {
  return !!getToken();
}
