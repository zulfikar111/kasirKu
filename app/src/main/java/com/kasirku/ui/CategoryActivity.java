package com.kasirku.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.kasirku.R;
import com.kasirku.data.CategoryRepository;
import com.kasirku.data.DataCallback;
import com.kasirku.model.Category;
import com.kasirku.util.ViewUtil;

import java.util.ArrayList;
import java.util.List;

/** Kelola kategori barang (khusus admin). */
public class CategoryActivity extends BaseActivity {

  private interface NameAction {
    void apply(String name, DataCallback<Void> callback);
  }

  private final CategoryRepository repository = new CategoryRepository();
  private final List<Category> categories = new ArrayList<>();
  private final List<String> names = new ArrayList<>();
  private ArrayAdapter<String> adapter;

  private ProgressBar progress;
  private TextView tvEmpty;

  @Override
  protected boolean requiresAdmin() {
    return true;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (!accessGranted) {
      return;
    }
    setContentView(R.layout.activity_category);
    ViewUtil.applySystemBarPadding(findViewById(R.id.main));

    MaterialToolbar toolbar = findViewById(R.id.toolbar);
    toolbar.setNavigationOnClickListener(v -> finish());

    progress = findViewById(R.id.progress);
    tvEmpty = findViewById(R.id.tvEmpty);
    ListView list = findViewById(R.id.listCategories);
    ExtendedFloatingActionButton fabAdd = findViewById(R.id.fabAdd);

    adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
    list.setAdapter(adapter);
    list.setOnItemClickListener((parent, view, position, id) ->
      showOptions(categories.get(position)));

    fabAdd.setOnClickListener(v -> showNameDialog("Tambah Kategori", "",
      (name, cb) -> repository.add(name, categories, cb)));

    load();
  }

  private void load() {
    progress.setVisibility(View.VISIBLE);
    repository.loadAll(new DataCallback<List<Category>>() {
      @Override
      public void onSuccess(List<Category> result) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        progress.setVisibility(View.GONE);
        categories.clear();
        categories.addAll(result);
        names.clear();
        for (Category c : result) {
          names.add(c.name == null ? "(tanpa nama)" : c.name);
        }
        adapter.notifyDataSetChanged();
        tvEmpty.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
      }

      @Override
      public void onError(String message) {
        if (isFinishing() || isDestroyed()) {
          return;
        }
        progress.setVisibility(View.GONE);
        Toast.makeText(CategoryActivity.this, message, Toast.LENGTH_LONG).show();
      }
    });
  }

  private void showOptions(Category category) {
    new MaterialAlertDialogBuilder(this)
      .setTitle(category.name)
      .setItems(new String[]{"Ubah nama", "Hapus"}, (dialog, which) -> {
        if (which == 0) {
          showNameDialog("Ubah Nama Kategori", category.name == null ? "" : category.name,
            (name, cb) -> repository.rename(category.id, name, categories, cb));
        } else {
          confirmDelete(category);
        }
      })
      .show();
  }

  private void confirmDelete(Category category) {
    new MaterialAlertDialogBuilder(this)
      .setTitle("Hapus kategori")
      .setMessage("Hapus kategori \"" + category.name + "\"?")
      .setNegativeButton("Batal", null)
      .setPositiveButton("Hapus", (dialog, which) ->
        repository.delete(category.id, new DataCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            if (isFinishing() || isDestroyed()) {
              return;
            }
            Toast.makeText(CategoryActivity.this,
              "Kategori dihapus.", Toast.LENGTH_SHORT).show();
            load();
          }

          @Override
          public void onError(String message) {
            if (isFinishing() || isDestroyed()) {
              return;
            }
            Toast.makeText(CategoryActivity.this,
              message, Toast.LENGTH_LONG).show();
          }
        }))
      .show();
  }

  /** Dialog isian nama. Jika gagal (misalnya nama kembar), dialog dibuka lagi dengan isian tadi. */
  private void showNameDialog(String title, String initial, NameAction action) {
    View view = getLayoutInflater().inflate(R.layout.dialog_input, (ViewGroup) null);
    TextInputEditText input = view.findViewById(R.id.inputEdit);
    input.setText(initial);
    input.setSelection(initial.length());

    new MaterialAlertDialogBuilder(this)
      .setTitle(title)
      .setView(view)
      .setNegativeButton("Batal", null)
      .setPositiveButton("Simpan", (dialog, which) -> {
        String name = input.getText() == null ? "" : input.getText().toString();
        action.apply(name, new DataCallback<Void>() {
          @Override
          public void onSuccess(Void result) {
            if (isFinishing() || isDestroyed()) {
              return;
            }
            Toast.makeText(CategoryActivity.this,
              "Kategori disimpan.", Toast.LENGTH_SHORT).show();
            load();
          }

          @Override
          public void onError(String message) {
            if (isFinishing() || isDestroyed()) {
              return;
            }
            Toast.makeText(CategoryActivity.this,
              message, Toast.LENGTH_LONG).show();
            showNameDialog(title, name, action);
          }
        });
      })
      .show();
  }
}
