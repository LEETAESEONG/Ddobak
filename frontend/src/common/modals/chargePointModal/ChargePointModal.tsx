import { useEffect, useState } from 'react';
import ReactModal from 'react-modal';
import PaymentComponent from 'componentPages/myPagePointPage/myPagePointPageComponents/PaymentComponent';
import AlertCustomModal from '../alertCustomModal/AlertCustomModal';
import classes from './ChargePointModal.module.css';

import { useSelector } from 'react-redux';
import { useDispatch } from 'react-redux';
import { chargePointModalActions } from 'store/chargePointModalSlice';
import { AiOutlineClose, AiFillCloseCircle } from 'react-icons/ai';
import { borderColor } from 'common/colors/CommonColors';
import { transactionChargeAPI } from 'https/utils/TransactionFunction';

interface ChargeModalState {
  chargePoint: {
    chargePointVisible: boolean;
    myPoint: number;
    nickname: string;
    isModal: boolean;
  };
}

const ChargePointModal: React.FC = () => {
  useEffect(() => {
    ReactModal.setAppElement('body'); // body나 다른 id를 사용할 수 있습니다.
  }, []);
  const dispatch = useDispatch();
  const clickChargeHandler = () => {
    dispatch(chargePointModalActions.toggle());
  };
  const showCharge = useSelector((state: ChargeModalState) => state.chargePoint.chargePointVisible);
  const isModal = useSelector((state: ChargeModalState) => state.chargePoint.isModal);
  const closeModal = () => {
    clickChargeHandler();
  };

  const nickname = useSelector((state: ChargeModalState) => state.chargePoint.nickname);
  const currentPoint = useSelector((state: ChargeModalState) => state.chargePoint.myPoint);
  const [chargePoint, setChargePoint] = useState<number>(0);
  const [totalPoint, setTotalPoint] = useState<number>(0);
  useEffect(() => {
    setTotalPoint(currentPoint);
  }, [currentPoint]);
  const howMuchCharge = (value: number) => {
    setChargePoint(chargePoint + value);
    setTotalPoint(totalPoint + value);
  };
  const removeCharge = () => {
    setChargePoint(0);
    setTotalPoint(currentPoint);
  };
  // 포인트 상태 관리

  // 포트원 결제 창 결제 결과 로직
  const handlePaymentSuccess = (response: any) => {
    // 결제 성공 시 필요한 로직을 실행
    transactionChargeAPI(chargePoint)
      .then(async (r) => {
        // DB에 API를 실행하고
        // 충전이 완료되었다는 모달이 필요한가.
        // 결제창이 닫힌다

        // 마이페이지에서 충전 할 때만 새로고침
        setChargePoint(0);
        dispatch(chargePointModalActions.chargePlus());
        closeModal();
        if (!isModal) {
          window.location.reload();
        }
      })
      .catch((e) => {
        console.error(e);
      });
  };

  const handlePaymentFailure = (error: any) => {
    if (chargePoint === 0) {
      handleNoChangePoint();
      return; // 조기 반환을 통해 이후 로직을 실행하지 않습니다.
    }
    console.error('Payment Failure:', error);
    // 결제 실패 시 필요한 로직을 실행
  };

  const handlePaymentCancel = (cancelData: any) => {
    // console.log('Payment Cancelled:', cancelData);
    // 결제 취소 시 필요한 로직을 실행
  };

  const [showAlertModal, setShowAlertModal] = useState(false);
  const handleNoChangePoint = () => {
    setShowAlertModal(true); //
  };

  return (
    <ReactModal
      isOpen={showCharge}
      onRequestClose={closeModal}
      shouldCloseOnOverlayClick={true}
      className={classes.overLay}
      style={{
        content: {
          zIndex: 10005,
        },
        overlay: {
          zIndex: 10004, // NavBar보다 1 높게 설정
          // ... other styles
        },
      }}
    >
      <div className={classes.modalContainer}>
        <div className={classes.topBox}>
          <AiOutlineClose size={30} className={classes.closeIcon} onClick={closeModal} />
        </div>
        <div className={classes.middleBox}>
          <div
            className={classes.innerBox}
            style={{
              borderBottomStyle: 'solid',
              borderBottomWidth: 2,
              borderBottomColor: borderColor,
            }}
          >
            <p className={classes.innerText}>현재 포인트</p>
            <p className={classes.innerText}>{currentPoint.toLocaleString()} P</p>
          </div>
          <div
            className={classes.innerMiddleBox}
            style={{ height: 40, justifyContent: 'space-between' }}
          >
            <p className={classes.innerText}>충전할 포인트</p>
            <p className={classes.innerNormalText}>충전은 5,000 단위로 가능합니다.</p>
          </div>
          <div
            className={classes.innerMiddleBox}
            style={{ height: 60, justifyContent: 'flex-end' }}
          >
            <div className={classes.chargeBox}>
              <p className={classes.chargeText}>{chargePoint.toLocaleString()} P</p>
              <div className={classes.removeImgBox}>
                <AiFillCloseCircle size={32} color={borderColor} onClick={removeCharge} />
              </div>
            </div>
          </div>
          <div
            className={classes.innerMiddleBox}
            style={{ height: 40, justifyContent: 'flex-end' }}
          >
            <button
              className={classes.valueBtn}
              onClick={() => {
                howMuchCharge(5000);
              }}
            >
              + 5,000
            </button>
            <button
              className={classes.valueBtn}
              onClick={() => {
                howMuchCharge(10000);
              }}
            >
              + 10,000
            </button>
            <button
              className={classes.valueBtn}
              onClick={() => {
                howMuchCharge(50000);
              }}
            >
              + 50,000
            </button>
          </div>

          <div
            className={classes.innerBox}
            style={{ borderTopStyle: 'solid', borderTopWidth: 2, borderTopColor: borderColor }}
          >
            <p className={classes.innerText}>총 포인트</p>
            <p className={classes.innerText}>{totalPoint.toLocaleString()} P</p>
          </div>
        </div>
        <div className={classes.bottomBox}>
          {/* 포트원 결제 창 */}
          <PaymentComponent
            nickname={nickname}
            amount={chargePoint}
            onPaymentSuccess={handlePaymentSuccess}
            onPaymentFailure={handlePaymentFailure}
            onPaymentCancel={handlePaymentCancel}
          />
        </div>
      </div>
      <AlertCustomModal
        show={showAlertModal}
        onHide={() => setShowAlertModal(false)}
        message1="충전할 금액을 선택해 주세요!"
        message2=""
        btnName="확인"
      />
    </ReactModal>
  );
};
export default ChargePointModal;
