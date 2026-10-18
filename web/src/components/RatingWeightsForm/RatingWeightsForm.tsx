import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Alert, Button, Spin } from 'antd';
import { useToast } from '../feedback/useToast';
import styles from '../SettingsModal/Settings.module.css';
import { RatingWeights } from '../../interface/RatingWeights';
import {
  describeRatingWeightsFailure,
  getRatingWeights,
  updateRatingWeights,
} from '../../services/ratingWeightsApi';
import WeightPairSlider from './WeightPairSlider';

interface RatingWeightsFormProps {
  projectName: string;
  onSaved: () => void;
  onCancel: () => void;
}

const ELECTION_EXPLANATION =
  'Which class represents each module. Saving a change re-elects every module except those ' +
  'whose reference classes you chose by hand.';

const RECOMMENDATION_EXPLANATION =
  'Which refactoring is worth making. Saving a change re-rates the split, merge and move ' +
  'recommendations.';

const RatingWeightsForm = ({ projectName, onSaved, onCancel }: RatingWeightsFormProps) => {
  const queryClient = useQueryClient();
  const toast = useToast();
  const queryKey = ['ratingWeights', projectName];
  const saved = useQuery({ queryKey, queryFn: () => getRatingWeights(projectName) });
  // Null until a slider moves; the form shows the saved weights until then.
  const [draft, setDraft] = useState<RatingWeights | null>(null);
  const save = useMutation({
    mutationFn: (weights: RatingWeights) => updateRatingWeights(projectName, weights),
    onSuccess: (weights) => {
      queryClient.setQueryData(queryKey, weights);
      toast.success('Weights saved: modules re-elected and recommendations re-rated');
      onSaved();
    },
  });

  if (saved.isPending) return <Spin />;
  if (saved.isError) {
    return (
      <Alert type="error" showIcon message={describeRatingWeightsFailure(saved.error, 'load')} />
    );
  }

  const shown = draft ?? saved.data;
  return (
    <>
      <WeightPairSlider
        name="Election"
        explanation={ELECTION_EXPLANATION}
        pair={shown.election}
        onChange={(election) => setDraft({ ...shown, election })}
      />
      <WeightPairSlider
        name="Recommendation rate"
        explanation={RECOMMENDATION_EXPLANATION}
        pair={shown.recommendation}
        onChange={(recommendation) => setDraft({ ...shown, recommendation })}
      />
      {save.isError && (
        <Alert type="error" showIcon message={describeRatingWeightsFailure(save.error)} />
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

export default RatingWeightsForm;
