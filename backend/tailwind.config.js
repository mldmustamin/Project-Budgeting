import defaultTheme from 'tailwindcss/defaultTheme';

/** @type {import('tailwindcss').Config} */
export default {
    content: [
        './vendor/laravel/framework/src/Illuminate/Pagination/resources/views/*.blade.php',
        './storage/framework/views/*.php',
        './resources/**/*.blade.php',
        './resources/**/*.js',
        './resources/**/*.vue',
    ],
    theme: {
        extend: {
            fontFamily: {
                sans: ['Inter', 'Figtree', ...defaultTheme.fontFamily.sans],
            },
            colors: {
                brand: {
                    50:  '#e9f5ed',
                    100: '#c8e6d1',
                    200: '#a3d4b0',
                    300: '#7cc28c',
                    400: '#5eb472',
                    500: '#45a75d',
                    600: '#238b45',
                    700: '#1e7a3c',
                    800: '#196933',
                    900: '#125226',
                },
            },
        },
    },
    plugins: [],
};
