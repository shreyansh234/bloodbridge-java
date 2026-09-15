import React from "react";
import {createRoot} from "react-dom/client";
import BloodBridge from "./components/bloodbridge";
import "./app/globals.css";
createRoot(document.getElementById("root")!).render(<BloodBridge/>);
