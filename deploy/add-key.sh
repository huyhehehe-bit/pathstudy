#!/usr/bin/env bash
# Nạp khoá deploy vào máy chủ. Chạy trên VPS bằng root.
#   curl -fsSL https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/add-key.sh | bash
#
# Khoá CÔNG KHAI thì công khai được — biết nó cũng không vào được máy.
# Khoá riêng nằm ở máy phát triển. Gỡ quyền bất cứ lúc nào bằng cách xoá
# dòng tương ứng trong ~/.ssh/authorized_keys.
set -euo pipefail
URL=https://raw.githubusercontent.com/huyhehehe-bit/pathstudy/main/deploy/claude-deploy-key.pub
mkdir -p ~/.ssh && chmod 700 ~/.ssh
KEY=$(curl -fsSL "$URL")
grep -qxF "$KEY" ~/.ssh/authorized_keys 2>/dev/null || echo "$KEY" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
echo "DA_NAP_KHOA: $(ssh-keygen -lf <(echo "$KEY") | awk '{print $2}')"
