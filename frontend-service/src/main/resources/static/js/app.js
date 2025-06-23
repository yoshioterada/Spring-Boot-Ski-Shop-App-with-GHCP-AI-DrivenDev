/**
 * Azure SkiShop Frontend JavaScript
 */

class SkiShopApp {
    constructor() {
        this.init();
    }

    init() {
        this.bindEvents();
        this.updateCartCount();
        this.initAnimations();
        this.initTooltips();
    }

    bindEvents() {
        // カート追加ボタン
        document.addEventListener('click', (e) => {
            if (e.target.matches('.add-to-cart-btn') || e.target.closest('.add-to-cart-btn')) {
                this.handleAddToCart(e);
            }
        });

        // 検索フォーム
        const searchForm = document.querySelector('.search-form');
        if (searchForm) {
            searchForm.addEventListener('submit', this.handleSearch.bind(this));
        }

        // フィルター変更
        document.addEventListener('change', (e) => {
            if (e.target.matches('.filter-select')) {
                this.handleFilterChange(e);
            }
        });

        // ページネーション
        document.addEventListener('click', (e) => {
            if (e.target.matches('.pagination-link')) {
                this.handlePagination(e);
            }
        });

        // モーダル制御
        document.addEventListener('click', (e) => {
            if (e.target.matches('[data-bs-toggle="modal"]')) {
                this.handleModalToggle(e);
            }
        });
    }

    async handleAddToCart(e) {
        e.preventDefault();
        
        const button = e.target.closest('.add-to-cart-btn');
        const form = button.closest('form');
        const productId = form.querySelector('input[name="productId"]').value;
        const quantity = form.querySelector('input[name="quantity"]')?.value || 1;

        // ボタンを無効化
        button.disabled = true;
        button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 追加中...';

        try {
            const response = await fetch('/cart/add', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: new URLSearchParams({
                    productId: productId,
                    quantity: quantity
                })
            });

            if (response.ok) {
                this.showNotification('商品をカートに追加しました', 'success');
                this.updateCartCount();
                this.animateCartIcon();
            } else {
                throw new Error('カートへの追加に失敗しました');
            }
        } catch (error) {
            this.showNotification(error.message, 'error');
        } finally {
            // ボタンを元に戻す
            setTimeout(() => {
                button.disabled = false;
                button.innerHTML = '<i class="fas fa-cart-plus"></i> カートに追加';
            }, 1000);
        }
    }

    handleSearch(e) {
        const searchInput = e.target.querySelector('input[name="q"]');
        const query = searchInput.value.trim();
        
        if (!query) {
            e.preventDefault();
            this.showNotification('検索キーワードを入力してください', 'warning');
            searchInput.focus();
        }
    }

    handleFilterChange(e) {
        const select = e.target;
        const form = select.closest('form');
        if (form) {
            form.submit();
        }
    }

    handlePagination(e) {
        e.preventDefault();
        const link = e.target;
        const url = link.href;
        
        // ページ読み込み表示
        this.showLoading();
        
        window.location.href = url;
    }

    handleModalToggle(e) {
        const modalId = e.target.getAttribute('data-bs-target');
        const modal = document.querySelector(modalId);
        
        if (modal) {
            const modalInstance = new bootstrap.Modal(modal);
            modalInstance.show();
        }
    }

    async updateCartCount() {
        try {
            const response = await fetch('/cart/count');
            if (response.ok) {
                const data = await response.json();
                const cartCountElement = document.getElementById('cart-count');
                if (cartCountElement) {
                    cartCountElement.textContent = data.count;
                    
                    // カート数が0の場合は非表示
                    if (data.count === 0) {
                        cartCountElement.style.display = 'none';
                    } else {
                        cartCountElement.style.display = 'inline-block';
                    }
                }
            }
        } catch (error) {
            console.warn('カートアイテム数の取得に失敗しました:', error);
        }
    }

    animateCartIcon() {
        const cartIcon = document.querySelector('.navbar-nav .fa-shopping-cart');
        if (cartIcon) {
            cartIcon.classList.add('animate__animated', 'animate__bounce');
            setTimeout(() => {
                cartIcon.classList.remove('animate__animated', 'animate__bounce');
            }, 1000);
        }
    }

    showNotification(message, type = 'info') {
        const alertClass = type === 'success' ? 'alert-success' :
                          type === 'error' ? 'alert-danger' :
                          type === 'warning' ? 'alert-warning' : 'alert-info';

        const alertHtml = `
            <div class="alert ${alertClass} alert-dismissible fade show" role="alert">
                ${message}
                <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
            </div>
        `;

        // 既存のアラートコンテナを探すか作成
        let alertContainer = document.querySelector('.alert-container');
        if (!alertContainer) {
            alertContainer = document.createElement('div');
            alertContainer.className = 'alert-container';
            alertContainer.style.cssText = `
                position: fixed;
                top: 80px;
                right: 20px;
                z-index: 9999;
                max-width: 400px;
            `;
            document.body.appendChild(alertContainer);
        }

        // アラートを追加
        const alertElement = document.createElement('div');
        alertElement.innerHTML = alertHtml;
        alertContainer.appendChild(alertElement.firstElementChild);

        // 5秒後に自動削除
        setTimeout(() => {
            const alert = alertContainer.querySelector('.alert');
            if (alert) {
                alert.remove();
            }
        }, 5000);
    }

    showLoading() {
        const loadingHtml = `
            <div class="loading-overlay" style="
                position: fixed;
                top: 0;
                left: 0;
                width: 100%;
                height: 100%;
                background: rgba(255, 255, 255, 0.8);
                display: flex;
                justify-content: center;
                align-items: center;
                z-index: 9999;
            ">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">読み込み中...</span>
                </div>
            </div>
        `;
        
        document.body.insertAdjacentHTML('beforeend', loadingHtml);
    }

    hideLoading() {
        const loadingOverlay = document.querySelector('.loading-overlay');
        if (loadingOverlay) {
            loadingOverlay.remove();
        }
    }

    initAnimations() {
        // スクロールアニメーション
        const observerOptions = {
            threshold: 0.1,
            rootMargin: '0px 0px -50px 0px'
        };

        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.classList.add('fade-in');
                }
            });
        }, observerOptions);

        // アニメーション対象要素を監視
        document.querySelectorAll('.card, .feature-icon, .hero-section').forEach(el => {
            observer.observe(el);
        });
    }

    initTooltips() {
        // Bootstrap tooltipを初期化
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        tooltipTriggerList.map(function (tooltipTriggerEl) {
            return new bootstrap.Tooltip(tooltipTriggerEl);
        });
    }

    // 商品比較機能
    compareProducts() {
        const compareCheckboxes = document.querySelectorAll('.compare-checkbox:checked');
        const productIds = Array.from(compareCheckboxes).map(cb => cb.value);
        
        if (productIds.length < 2) {
            this.showNotification('比較するには少なくとも2つの商品を選択してください', 'warning');
            return;
        }
        
        if (productIds.length > 4) {
            this.showNotification('比較できるのは最大4つの商品までです', 'warning');
            return;
        }
        
        const compareUrl = `/products/compare?ids=${productIds.join(',')}`;
        window.open(compareUrl, '_blank');
    }

    // ウィッシュリスト機能
    async toggleWishlist(productId) {
        try {
            const response = await fetch('/wishlist/toggle', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify({ productId: productId })
            });

            if (response.ok) {
                const data = await response.json();
                const heartIcon = document.querySelector(`[data-product-id="${productId}"] .heart-icon`);
                
                if (heartIcon) {
                    if (data.inWishlist) {
                        heartIcon.classList.remove('far');
                        heartIcon.classList.add('fas', 'text-danger');
                        this.showNotification('ウィッシュリストに追加しました', 'success');
                    } else {
                        heartIcon.classList.remove('fas', 'text-danger');
                        heartIcon.classList.add('far');
                        this.showNotification('ウィッシュリストから削除しました', 'info');
                    }
                }
            }
        } catch (error) {
            this.showNotification('ウィッシュリストの更新に失敗しました', 'error');
        }
    }

    // レビュー機能
    initReviewSystem() {
        const reviewForm = document.querySelector('#review-form');
        if (reviewForm) {
            reviewForm.addEventListener('submit', this.handleReviewSubmit.bind(this));
        }

        // 星評価
        document.addEventListener('click', (e) => {
            if (e.target.matches('.rating-star')) {
                this.handleStarRating(e);
            }
        });
    }

    async handleReviewSubmit(e) {
        e.preventDefault();
        
        const form = e.target;
        const formData = new FormData(form);
        
        try {
            const response = await fetch(form.action, {
                method: 'POST',
                body: formData,
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (response.ok) {
                this.showNotification('レビューを投稿しました', 'success');
                form.reset();
                // レビュー一覧を更新
                this.refreshReviews();
            } else {
                throw new Error('レビューの投稿に失敗しました');
            }
        } catch (error) {
            this.showNotification(error.message, 'error');
        }
    }

    handleStarRating(e) {
        const star = e.target;
        const rating = parseInt(star.dataset.rating);
        const container = star.closest('.star-rating');
        const ratingInput = container.querySelector('input[name="rating"]');
        
        // 評価を設定
        if (ratingInput) {
            ratingInput.value = rating;
        }
        
        // 星の表示を更新
        const stars = container.querySelectorAll('.rating-star');
        stars.forEach((s, index) => {
            if (index < rating) {
                s.classList.remove('far');
                s.classList.add('fas');
            } else {
                s.classList.remove('fas');
                s.classList.add('far');
            }
        });
    }

    async refreshReviews() {
        const productId = document.querySelector('[data-product-id]')?.dataset.productId;
        if (!productId) return;

        try {
            const response = await fetch(`/api/products/${productId}/reviews`);
            if (response.ok) {
                const reviews = await response.json();
                this.updateReviewsDisplay(reviews);
            }
        } catch (error) {
            console.warn('レビューの更新に失敗しました:', error);
        }
    }

    updateReviewsDisplay(reviews) {
        const reviewsContainer = document.querySelector('#reviews-container');
        if (!reviewsContainer) return;

        // レビュー表示を更新する実装
        // ... (レビューHTMLの生成と挿入)
    }
}

// ユーティリティ関数
const Utils = {
    formatPrice(price) {
        return new Intl.NumberFormat('ja-JP', {
            style: 'currency',
            currency: 'JPY'
        }).format(price);
    },

    formatDate(dateString) {
        return new Intl.DateTimeFormat('ja-JP', {
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        }).format(new Date(dateString));
    },

    debounce(func, wait) {
        let timeout;
        return function executedFunction(...args) {
            const later = () => {
                clearTimeout(timeout);
                func(...args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    },

    throttle(func, limit) {
        let inThrottle;
        return function() {
            const args = arguments;
            const context = this;
            if (!inThrottle) {
                func.apply(context, args);
                inThrottle = true;
                setTimeout(() => inThrottle = false, limit);
            }
        };
    }
};

// アプリケーション初期化
document.addEventListener('DOMContentLoaded', () => {
    window.SkiShopApp = new SkiShopApp();
    
    // ページ読み込み完了時にローディングを非表示
    window.addEventListener('load', () => {
        window.SkiShopApp.hideLoading();
    });
});
