export type MergeData = {
  source: string;
  target: string;
  onCancel?: () => void;
  onConfirm?: () => Promise<void>;
};
