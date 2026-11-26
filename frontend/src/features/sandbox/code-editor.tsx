"use client";

import { python } from "@codemirror/lang-python";
import { oneDark } from "@codemirror/theme-one-dark";
import CodeMirror from "@uiw/react-codemirror";

type CodeEditorProps = {
  value: string;
  onChange: (value: string) => void;
};

export default function CodeEditor({ value, onChange }: CodeEditorProps) {
  return (
    <CodeMirror
      value={value}
      onChange={onChange}
      extensions={[python()]}
      theme={oneDark}
      minHeight="360px"
      height="420px"
      aria-label="Trình soạn thảo mã Python"
      basicSetup={{
        lineNumbers: true,
        highlightActiveLineGutter: true,
        foldGutter: true,
        autocompletion: true,
        tabSize: 4,
      }}
    />
  );
}
