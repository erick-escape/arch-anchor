import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Checkbox, Spin } from 'antd';
import { useToast } from '../feedback/useToast';
import styles from '../SettingsModal/Settings.module.css';
import { AnalysisSettings } from '../../interface/AnalysisSettings';
import {
  describeAnalysisSettingsFailure,
  getAnalysisSettings,
  updateAnalysisSettings,
} from '../../services/analysisSettingsApi';
import SettingNumber from './SettingNumber';

interface AnalysisSettingsFormProps {
  projectName: string;
  onSaved: () => void;
  onCancel: () => void;
}

// What the analyses recommend and show for the project (#36). The defaults filter nothing.
const AnalysisSettingsForm = ({ projectName, onSaved, onCancel }: AnalysisSettingsFormProps) => {
  const queryClient = useQueryClient();
  const toast = useToast();
  const queryKey = ['analysisSettings', projectName];
  const saved = useQuery({ queryKey, queryFn: () => getAnalysisSettings(projectName) });
  // Null until a value changes; the form shows the saved settings until then.
  const [draft, setDraft] = useState<AnalysisSettings | null>(null);
  const save = useMutation({
    mutationFn: (settings: AnalysisSettings) => updateAnalysisSettings(projectName, settings),
    onSuccess: (settings) => {
      queryClient.setQueryData(queryKey, settings);
      toast.success('Settings saved: recommendations refreshed');
      onSaved();
    },
  });

  if (saved.isPending) return <Spin />;
  if (saved.isError) {
    return (
      <Alert type="error" showIcon message={describeAnalysisSettingsFailure(saved.error, 'load')} />
    );
  }

  const shown = draft ?? saved.data;
  const change = (changed: Partial<AnalysisSettings>) => setDraft({ ...shown, ...changed });
  const unlimitedTargets = shown.maxMoveTargetsPerClass === null;
  const unlimitedMergeLoss = shown.minMergeSimilarityChange === null;
  return (
    <>
      <SettingNumber
        label="Fewest classes in each half of a split"
        explanation="A split with a smaller half is not recommended. A one-class half scores similarity 1 by definition."
        value={shown.minSplitModuleClasses}
        min={1}
        precision={0}
        onChange={(minSplitModuleClasses) => change({ minSplitModuleClasses })}
      />
      <SettingNumber
        label="Fewest classes a move leaves in its module"
        explanation="A move that would leave its module with fewer classes is not recommended."
        value={shown.minSourceClassesAfterMove}
        min={1}
        precision={0}
        onChange={(minSourceClassesAfterMove) => change({ minSourceClassesAfterMove })}
      />
      <SettingNumber
        label="Move targets per class"
        explanation="Only each class's best-rated targets are recommended."
        value={shown.maxMoveTargetsPerClass}
        min={1}
        precision={0}
        disabled={unlimitedTargets}
        onChange={(maxMoveTargetsPerClass) => change({ maxMoveTargetsPerClass })}
      >
        <Checkbox
          checked={unlimitedTargets}
          onChange={(event) => change({ maxMoveTargetsPerClass: event.target.checked ? null : 1 })}
        >
          No limit
        </Checkbox>
      </SettingNumber>
      <SettingNumber
        label="Fewest package dependencies of a judged class"
        explanation="A class with fewer gets no move and no violation entries. Its module still counts its violations."
        value={shown.minClassDependencies}
        min={0}
        precision={0}
        onChange={(minClassDependencies) => change({ minClassDependencies })}
      />
      <SettingNumber
        label="Lowest rate of a shown split, merge or move"
        explanation="Violations have no rate and are always shown."
        value={shown.minRecommendationRate}
        min={0}
        max={1}
        step={0.05}
        precision={2}
        onChange={(minRecommendationRate) => change({ minRecommendationRate })}
      />
      <SettingNumber
        label="Lowest similarity change of a recommended merge"
        explanation="0 keeps out merges that lower similarity, recommended only for the violations they remove."
        value={shown.minMergeSimilarityChange}
        min={-1}
        max={1}
        step={0.05}
        precision={2}
        disabled={unlimitedMergeLoss}
        onChange={(minMergeSimilarityChange) => change({ minMergeSimilarityChange })}
      >
        <Checkbox
          checked={unlimitedMergeLoss}
          onChange={(event) =>
            change({ minMergeSimilarityChange: event.target.checked ? null : 0 })
          }
        >
          No limit
        </Checkbox>
      </SettingNumber>
      {save.isError && (
        <Alert type="error" showIcon message={describeAnalysisSettingsFailure(save.error)} />
      )}
      <div className={styles.actions}>
        <Button onClick={onCancel}>Cancel</Button>
        <Button type="primary" loading={save.isPending} onClick={() => save.mutate(shown)}>
          Save
        </Button>
      </div>
    </>
  );
};

export default AnalysisSettingsForm;
