"use client";

import { memo } from "react";
import { TrendingUp, TrendingDown, Activity } from "lucide-react";

const STOCKS = [
    { symbol: "NVDA", name: "Nvidia Corp", price: 894.20, change: "+4.2%", isHigh: true },
    { symbol: "AAPL", name: "Apple Inc.", price: 173.50, change: "+1.2%", isHigh: true },
    { symbol: "TSLA", name: "Tesla Inc.", price: 172.98, change: "-3.4%", isHigh: false },
    { symbol: "MSFT", name: "Microsoft", price: 415.10, change: "+0.8%", isHigh: true },
    { symbol: "META", name: "Meta Platforms", price: 485.50, change: "-1.1%", isHigh: false },
];

export const LiveStocksWidget = memo(function LiveStocksWidget() {
    return (
        <div className="bg-surface-container-lowest p-6 rounded-xl ghost-border ambient-shadow">
            <h2 className="text-xl font-bold text-on-surface mb-6 flex items-center gap-2">
                <Activity className="w-5 h-5 text-primary" /> Live Stocks
            </h2>
            <div className="space-y-4">
                {STOCKS.map(stock => (
                    <div key={stock.symbol} className="flex justify-between items-center p-3 rounded-xl hover:bg-surface-container-low hover:shadow-ambient transition-all group cursor-pointer border border-transparent hover:border-outline-variant/30">
                        <div className="flex items-center gap-3">
                            <div className={`w-10 h-10 rounded-full flex items-center justify-center text-xs font-bold shadow-sm ${stock.isHigh ? 'bg-primary-container text-on-primary-container' : 'bg-error-container text-on-error-container'}`}>
                                {stock.symbol[0]}
                            </div>
                            <div>
                                <div className="text-on-surface font-semibold text-sm group-hover:text-primary transition-colors">{stock.symbol}</div>
                                <div className="text-on-surface-variant text-xs">{stock.name}</div>
                            </div>
                        </div>
                        <div className="text-right">
                            <div className="text-on-surface font-mono text-sm font-medium">₹{stock.price.toFixed(2)}</div>
                            <div className={`text-xs font-bold flex items-center justify-end gap-1 ${stock.isHigh ? 'text-primary' : 'text-error'}`}>
                                {stock.isHigh ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                                {stock.change}
                            </div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
});
