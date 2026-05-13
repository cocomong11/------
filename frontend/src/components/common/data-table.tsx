import * as React from "react";
import { cn } from "@/lib/utils";
import { EmptyState } from "./empty-state";

export type DataTableColumn<T> = {
  key: string;
  header: React.ReactNode;
  cell: (row: T) => React.ReactNode;
  className?: string;
};

export function DataTable<T>({
  columns,
  emptyDescription = "표시할 데이터가 없습니다.",
  emptyTitle = "데이터가 없습니다",
  getRowKey,
  minWidth = 760,
  rows,
}: {
  columns: Array<DataTableColumn<T>>;
  emptyDescription?: string;
  emptyTitle?: string;
  getRowKey: (row: T) => string;
  minWidth?: number;
  rows: T[];
}) {
  if (rows.length === 0) {
    return <EmptyState description={emptyDescription} title={emptyTitle} />;
  }

  return (
    <div className="overflow-x-auto rounded-xl border border-border bg-card">
      <table className="w-full border-collapse text-[13px]" style={{ minWidth }}>
        <thead>
          <tr className="border-b border-border bg-card text-left text-[11px] font-semibold text-muted-foreground">
            {columns.map((column) => (
              <th key={column.key} className={cn("px-3 py-2", column.className)}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={getRowKey(row)} className="border-b border-secondary bg-card last:border-0 hover:bg-muted">
              {columns.map((column) => (
                <td key={column.key} className={cn("px-3 py-[11px] align-middle text-slate-700", column.className)}>
                  {column.cell(row)}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
