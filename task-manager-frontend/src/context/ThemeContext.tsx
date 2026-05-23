import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';

interface ThemeContextType {
    dark: boolean;
    setDark: (d: boolean) => void;
}

const ThemeContext = createContext<ThemeContextType>({ dark: false, setDark: () => {} });

export function ThemeProvider({ children }: { children: ReactNode }) {
    const [dark, setDarkState] = useState(() => {
        const saved = localStorage.getItem('theme');
        return saved ? saved === 'dark' : window.matchMedia('(prefers-color-scheme: dark)').matches;
    });

    useEffect(() => {
        document.documentElement.classList.toggle('dark', dark);
        localStorage.setItem('theme', dark ? 'dark' : 'light');
    }, [dark]);

    return (
        <ThemeContext.Provider value={{ dark, setDark: setDarkState }}>
            {children}
        </ThemeContext.Provider>
    );
}

export const useTheme = () => useContext(ThemeContext);