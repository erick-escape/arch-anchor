import './App.css';
import { BrowserRouter as Router, Route, Routes } from 'react-router-dom';
import ProjectsPage from './screens/Projects/index.tsx';
import AnalyzePage from './screens/Analyze/index.tsx';

function App() {
    return (
        <Router>
            <Routes>
                <Route path="/" element={<ProjectsPage />} />
                <Route path="/analyze/:projectName" element={<AnalyzePage />} />
            </Routes>
        </Router>
    );
}

export default App;
