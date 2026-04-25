const AUTH_KEY = "nexeval-auth";

export function saveLogin(loginInfo) {
  if (typeof window === "undefined") {
    return;
  }

  const payload = {
    cardNo: loginInfo?.id || loginInfo?.cardNo || "",
    name: loginInfo?.name || "",
    sex: typeof loginInfo?.sex === "boolean" ? loginInfo.sex : null,
    type: loginInfo?.type || "",
    phone: loginInfo?.phone || "",
    email: loginInfo?.email || "",
    avatarUrl: loginInfo?.avatarUrl || "",
    studentInfo: loginInfo?.studentInfo || null,
    teacherInfo: loginInfo?.teacherInfo || null,
    loginAt: Date.now()
  };

  window.sessionStorage.setItem(AUTH_KEY, JSON.stringify(payload));
}

export function getLogin() {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.sessionStorage.getItem(AUTH_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function isLoggedIn() {
  const info = getLogin();
  return Boolean(info?.cardNo);
}

export function clearLogin() {
  if (typeof window === "undefined") {
    return;
  }

  window.sessionStorage.removeItem(AUTH_KEY);
}
