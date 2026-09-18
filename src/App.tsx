import React from 'react';

export function App() {
  return (
    <div className="min-h-screen bg-[#0b0b14] text-[#eceef2] flex flex-col items-center justify-center p-6 text-center">
      <div className="max-w-md w-full p-8 rounded-2xl bg-[#15151f] border border-[#2a2a3a]">
        <h1 className="text-xl font-bold mb-3 text-[#9b8bff]">リポジトリを初期化しました</h1>
        <p className="text-sm text-[#8a8ea3] mb-4 leading-relaxed">
          不要なファイルをすべて削除し、クリーンな最小構成にリセットしました。
        </p>
        <p className="text-xs text-[#5aa9ff] bg-[#1b1b28] py-2 px-3 rounded-lg border border-[#2a2a3a]">
          ファイルのアップロードや差し替えの準備が完了しています。ファイルを配置後にお知らせください。
        </p>
      </div>
    </div>
  );
}
