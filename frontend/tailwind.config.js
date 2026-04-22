/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        // n11 brand palette
        n11: {
          purple: '#6633CC',
          orange: '#F9A825',
          dark: '#1A1A2E',
          gray: '#F5F5F7',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
