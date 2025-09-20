declare global {
  interface Window {
    grecaptcha: {
      render: (container: string | Element, params: any) => number;
      reset: (widgetId?: number) => void;
      getResponse: (widgetId?: number) => string;
    };
    onRecaptchaLoad: () => void;
  }
}
export {};
