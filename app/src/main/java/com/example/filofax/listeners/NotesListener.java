package com.example.filofax.listeners;

import com.example.filofax.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note,int position);
}
