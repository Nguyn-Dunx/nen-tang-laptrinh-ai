package vn.edu.codelearn.platform.identity;

record Account(long id, String email, String passwordHash, Role role) {

    enum Role {
        LEARNER,
        AUTHOR,
        ADMIN
    }

    @Override
    public String toString() {
        return "Account[id=" + id + ", role=" + role + "]";
    }
}
