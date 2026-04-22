import { FormEvent, useState } from 'react';
import { useApi } from '../../shared/hooks/useApi';
import { Button } from '../../shared/ui/Button';
import { Card } from '../../shared/ui/Card';
import { RatingStars } from '../../shared/ui/RatingStars';
import { useToast } from '../../shared/providers/ToastProvider';
import { errorMessage } from '../../shared/api/problem';
import { formatDate } from '../../shared/utils/format';
import { useAuthStore } from '../auth/store';
import { reviewApi } from './api';

interface Props {
  productId: number;
}

export function ReviewList({ productId }: Props) {
  const authed = useAuthStore((s) => s.isAuthenticated());
  const toast = useToast();

  const reviewsQ = useApi(() => reviewApi.listByProduct(productId), [productId]);
  const statsQ = useApi(() => reviewApi.statsByProduct(productId), [productId]);

  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [formOpen, setFormOpen] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await reviewApi.create({ productId, rating, comment });
      toast.success('Yorumun eklendi.');
      setComment('');
      setRating(5);
      setFormOpen(false);
      await reviewsQ.refetch();
      await statsQ.refetch();
    } catch (err) {
      toast.error(errorMessage(err, 'Yorum eklenemedi.'));
    } finally {
      setSubmitting(false);
    }
  };

  const stats = statsQ.data;
  const reviews = reviewsQ.data?.content ?? [];

  return (
    <div className="mt-8 space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-bold">Değerlendirmeler</h2>
        {stats && stats.count > 0 && (
          <div className="flex items-center gap-2 text-sm">
            <RatingStars value={stats.averageRating} />
            <span className="font-bold">{stats.averageRating.toFixed(1)}</span>
            <span className="text-gray-500">({stats.count} yorum)</span>
          </div>
        )}
      </div>

      {authed ? (
        formOpen ? (
          <Card className="p-4">
            <form onSubmit={handleSubmit} className="space-y-3">
              <div>
                <span className="text-sm font-medium text-gray-700 block mb-1">Puanın</span>
                <RatingStars value={rating} size={24} interactive onChange={setRating} />
              </div>
              <label className="block">
                <span className="text-sm font-medium text-gray-700">Yorumun</span>
                <textarea
                  required
                  rows={3}
                  maxLength={2000}
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  className="mt-1 w-full rounded border border-gray-300 px-3 py-2 focus:outline-none focus:ring-2 focus:ring-n11-purple"
                />
              </label>
              <div className="flex gap-2">
                <Button type="submit" loading={submitting}>
                  Yorumu Gönder
                </Button>
                <Button type="button" variant="ghost" onClick={() => setFormOpen(false)}>
                  İptal
                </Button>
              </div>
            </form>
          </Card>
        ) : (
          <Button variant="ghost" onClick={() => setFormOpen(true)}>
            ✍️ Yorum Yaz
          </Button>
        )
      ) : (
        <div className="text-sm text-gray-500">Yorum yazmak için giriş yapmalısın.</div>
      )}

      {reviewsQ.loading ? (
        <div className="text-gray-500">Yorumlar yükleniyor...</div>
      ) : reviews.length === 0 ? (
        <div className="text-gray-500 text-sm">Henüz yorum yapılmamış.</div>
      ) : (
        <div className="space-y-3">
          {reviews.map((r) => (
            <Card key={r.id} className="p-4">
              <div className="flex items-center gap-2 mb-1">
                <span className="font-semibold">{r.userName}</span>
                <RatingStars value={r.rating} size={14} />
                <span className="text-xs text-gray-400 ml-auto">{formatDate(r.createdAt)}</span>
              </div>
              <div className="text-sm text-gray-700 whitespace-pre-wrap">{r.comment}</div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
