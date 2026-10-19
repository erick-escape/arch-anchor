import { BrowserRouter } from 'react-router-dom';
import { AppRoutes } from './AppRoutes';
import { CommandPaletteProvider } from './components/CommandPalette/CommandPaletteProvider';

function App() {
  return (
    <BrowserRouter>
      <CommandPaletteProvider>
        <AppRoutes />
      </CommandPaletteProvider>
    </BrowserRouter>
  );
}

export default App;
