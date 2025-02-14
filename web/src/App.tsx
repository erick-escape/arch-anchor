import './App.css';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import ProjectsPage from './screens/Projects/index.tsx';
import AnalyzePageWithProvider from './screens/Analyze';

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<ProjectsPage />} />
                <Route path="/analyze/:projectName" element={<AnalyzePageWithProvider />} />
            </Routes>
        </Router>
    );
}

export default App;
