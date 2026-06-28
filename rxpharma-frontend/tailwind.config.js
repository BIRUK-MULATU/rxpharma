/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50:  '#eef2f7',
          100: '#d5dde8',
          500: '#1e3a5f',
          600: '#162d4a',
          700: '#0f1f35',
          900: '#0a1628',
        },
        accent: {
          50:  '#fdf8ed',
          100: '#f9edd0',
          500: '#c9952e',
          600: '#b07d1f',
          700: '#8e6416',
        },
      }
    },
  },
  plugins: [],
}