export function authErrorMessage(error, fallback = 'Unable to complete your request. Please try again.') {
  const status = error?.response?.status;
  if (!error?.response || status >= 500) {
    return 'We cannot connect to the sign-in service right now. Please try again shortly.';
  }
  if (status === 429) return 'Too many attempts. Please wait a moment before trying again.';
  const message = error.response.data?.message || error.response.data?.error;
  return typeof message === 'string' && message.trim() ? message : fallback;
}
