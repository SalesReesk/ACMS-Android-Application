package com.example.araspoint_map.frontend.activity

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.araspoint_map.R
import com.example.araspoint_map.backend.model.User
import com.example.araspoint_map.backend.model.shortDepartment
import com.google.android.material.button.MaterialButton

class UserAdapter(
    private var users: List<User>,
    private val onEditRoleClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.imgUserAvatar)
        val txtName: TextView = view.findViewById(R.id.txtUserName)
        val txtEmail: TextView = view.findViewById(R.id.txtUserEmail)
        val txtRole: TextView = view.findViewById(R.id.txtUserRoleBadge)
        val txtDept: TextView = view.findViewById(R.id.txtUserDept)
        val btnChangeRole: MaterialButton = view.findViewById(R.id.btnChangeRole)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDeleteUser)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.txtName.text = user.name.ifBlank { user.email.substringBefore("@") }
        holder.txtEmail.text = user.email
        holder.txtRole.text = user.role.name
        holder.txtDept.text = "Dept: ${user.shortDepartment} • ID: ${user.idNumber.ifBlank { "—" }}"

        if (user.photo.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(user.photo)
                .placeholder(R.drawable.user_icon)
                .into(holder.imgAvatar)
        } else {
            holder.imgAvatar.setImageResource(R.drawable.user_icon)
        }

        holder.btnChangeRole.setOnClickListener { onEditRoleClick(user) }
        holder.btnDelete.setOnClickListener { onDeleteClick(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateList(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }
}
