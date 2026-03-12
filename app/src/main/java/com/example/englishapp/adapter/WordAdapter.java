package com.example.englishapp.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.englishapp.R;
import com.example.englishapp.entity.Word;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WordAdapter extends RecyclerView.Adapter<WordAdapter.WordViewHolder> {

    private List<Word> words = new ArrayList<>();
    private OnItemClickListener listener;
    private OnFavoriteClickListener favoriteListener;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    public interface OnItemClickListener {
        void onItemClick(Word word);
    }

    public interface OnFavoriteClickListener {
        void onFavoriteClick(Word word, int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnFavoriteClickListener(OnFavoriteClickListener listener) {
        this.favoriteListener = listener;
    }

    @NonNull
    @Override
    public WordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_word, parent, false);
        return new WordViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull WordViewHolder holder, int position) {
        Word currentWord = words.get(position);
        holder.textViewWord.setText(currentWord.getEnglishWord());
        holder.textViewMeaning.setText(currentWord.getChineseMeaning());
        holder.textViewPhonetic.setText("/" + currentWord.getPhonetic() + "/");

        // 显示标签
        String tags = currentWord.getTags();
        if (tags != null && !tags.isEmpty()) {
            holder.textViewTags.setText(tags.replace(",", " • "));
            holder.textViewTags.setVisibility(View.VISIBLE);
        } else {
            holder.textViewTags.setVisibility(View.GONE);
        }

        // 显示掌握程度
        holder.textViewMastery.setText(currentWord.getMasteryStars());

        // 显示复习信息
        if (currentWord.needsReview()) {
            holder.textViewNextReview.setText("需要复习");
            holder.textViewNextReview.setTextColor(holder.itemView.getContext().getColor(android.R.color.holo_red_dark));
        } else {
            holder.textViewNextReview.setText("下次: " + dateFormat.format(currentWord.getNextReview()));
            holder.textViewNextReview.setTextColor(holder.itemView.getContext().getColor(android.R.color.darker_gray));
        }

        // 收藏状态
        holder.textViewFavorite.setText(currentWord.isFavorite() ? "★" : "☆");
        holder.textViewFavorite.setTextColor(currentWord.isFavorite() ?
                holder.itemView.getContext().getColor(android.R.color.holo_orange_dark) :
                holder.itemView.getContext().getColor(android.R.color.darker_gray));

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(currentWord);
            }
        });

        holder.textViewFavorite.setOnClickListener(v -> {
            if (favoriteListener != null) {
                favoriteListener.onFavoriteClick(currentWord, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return words.size();
    }

    public void setWords(List<Word> words) {
        this.words = words;
        notifyDataSetChanged();
    }

    public Word getWordAt(int position) {
        return words.get(position);
    }

    static class WordViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewWord;
        private final TextView textViewMeaning;
        private final TextView textViewPhonetic;
        private final TextView textViewTags;
        private final TextView textViewMastery;
        private final TextView textViewNextReview;
        private final TextView textViewFavorite;
        private final CardView cardView;

        public WordViewHolder(@NonNull View itemView) {
            super(itemView);
            textViewWord = itemView.findViewById(R.id.tv_word);
            textViewMeaning = itemView.findViewById(R.id.tv_meaning);
            textViewPhonetic = itemView.findViewById(R.id.tv_phonetic);
            textViewTags = itemView.findViewById(R.id.tv_tags);
            textViewMastery = itemView.findViewById(R.id.tv_mastery);
            textViewNextReview = itemView.findViewById(R.id.tv_next_review);
            textViewFavorite = itemView.findViewById(R.id.tv_favorite);
            cardView = itemView.findViewById(R.id.card_view);
        }
    }
}