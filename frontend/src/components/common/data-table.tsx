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
    <div className="overflow-x-auto rounded-md border">
      <table className="w-full border-collapse text-sm" style={{ minWidth }}>
        <thead>
          <tr className="border-b bg-muted/60 text-left text-xs font-semibold uppercase tracking-[0.04em] text-muted-foreground">
            {columns.map((column) => (
              <th key={column.key} className={cn("px-4 py-3", column.className)}>
                {column.header}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={getRowKey(row)} className="border-b bg-card last:border-0 hover:bg-secondary/40">
              {columns.map((column) => (
                <td key={column.key} className={cn("px-4 py-3 align-middle", column.className)}>
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
